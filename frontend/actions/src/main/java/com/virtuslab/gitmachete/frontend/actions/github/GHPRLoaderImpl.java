package com.virtuslab.gitmachete.frontend.actions.github;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.concurrency.Semaphore;
import io.vavr.Value;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequest;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestShort;
import org.jetbrains.plugins.github.authentication.GHAccountsUtil;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;
import org.jetbrains.plugins.github.pullrequest.data.GHListLoader;
import org.jetbrains.plugins.github.pullrequest.data.GHPRListLoader;
import org.jetbrains.plugins.github.pullrequest.data.service.GHPRDetailsService;
import org.jetbrains.plugins.github.pullrequest.data.service.GHPRDetailsServiceImpl;
import org.jetbrains.plugins.github.pullrequest.ui.toolwindow.GHPRListSearchValue;
import org.jetbrains.plugins.github.util.GHCompatibilityUtil;
import org.jetbrains.plugins.github.util.GithubUrlUtil;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public class GHPRLoaderImpl implements IGHPRLoader {
  private final Project project;
  private final Disposable disposable;
  private final ProgressIndicator indicator;

  public GHPRLoaderImpl(Project project, Disposable disposable, ProgressIndicator indicator) {
    this.project = project;
    this.disposable = disposable;
    this.indicator = indicator;
  }

  @UIThreadUnsafe
  @Override
  public void run() {
    indicator.setFraction(0.0);
    val executor = getRequestExecutor(project);
    val coordinates = getRepositoryCoordinates(project);

    if (executor != null && coordinates != null) {
      GHPRListLoader ghprListLoader = new GHPRListLoader(ProgressManager.getInstance(), executor, coordinates);
      GHPRDetailsService ghprDetailsService = new GHPRDetailsServiceImpl(ProgressManager.getInstance(), executor, coordinates);
      Disposer.register(disposable, ghprListLoader);

      indicator.setFraction(0.1);
      val shortPrs = loadShortPRs(ghprListLoader);
      indicator.checkCanceled();
      indicator.setFraction(0.2);
      val pullRequests = loadPRDetails(shortPrs, ghprDetailsService);
      indicator.checkCanceled();
      indicator.setFraction(0.9);
      writeBranchLayout(pullRequests);
    }
  }

  @UIThreadUnsafe
  private List<GHPullRequestShort> loadShortPRs(GHPRListLoader ghprListLoader) {
    val semaphore = new Semaphore(1);
    ghprListLoader.setSearchQuery(GHPRListSearchValue.Companion.getDEFAULT().toQuery());
    ghprListLoader.addDataListener(disposable, new GHListLoader.ListDataListener() {
      //Remove when min version >= 2022.3
      @Override
      public void onAllDataRemoved() {}

      @Override
      public void onDataRemoved(@NotNull Object data) {}

      @Override
      public void onDataUpdated(int idx) {}

      @Override
      public void onDataAdded(int i) {
        if (ghprListLoader.canLoadMore() && !indicator.isCanceled()) {
          ghprListLoader.loadMore(false);
        } else {
          semaphore.up();
        }
      }
    });
    ghprListLoader.loadMore(false);
    semaphore.waitFor();
    return List.ofAll(ghprListLoader.getLoadedData());
  }

  @UIThreadUnsafe
  private List<GHPullRequest> loadPRDetails(List<GHPullRequestShort> pullRequestShorts, GHPRDetailsService ghprDetailsService) {
    val progressIndicator = new EmptyProgressIndicator();
    double prFraction = 0.8 / (pullRequestShorts.size() + 1);
    return pullRequestShorts.toStream()
        .map(x -> ghprDetailsService.loadDetails(progressIndicator, x)).map(x -> {
          indicator.checkCanceled();
          indicator.setFraction(indicator.getFraction() + prFraction);
          try {
            return Option.of(x.get());
          } catch (InterruptedException | ExecutionException e) {
            return Option.<GHPullRequest>none();
          }
        }).flatMap(Value::toStream).toList();

  }

  @UIThreadUnsafe
  private void writeBranchLayout(List<GHPullRequest> pullRequests) {
    val repository = project.getService(SelectedGitRepositoryProvider.class).getSelectedGitRepository();

    Map<String, GHPullRequest> requestMap = pullRequests.toMap(GHPullRequest::getHeadRefName, Function.identity());
    val macheteFilePath = Option.of(repository).map(GitVfsUtils::getMacheteFilePath).getOrNull();
    if (macheteFilePath != null) {
      val branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
      val branchLayoutWriter = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);
      try {
        BranchLayout branchLayout = branchLayoutReader.read(macheteFilePath);
        val newBranchLayout = branchLayout.map(entry -> {
          String annotation = requestMap.get(entry.getName()).map(x -> "PR #" + x.getNumber()).getOrNull();
          if (annotation == null) {
            annotation = entry.getCustomAnnotation();
          }
          return new BranchLayoutEntry(entry.getName(), annotation, entry.getChildren());
        });
        indicator.checkCanceled();
        indicator.setFraction(0.95);
        branchLayoutWriter.write(macheteFilePath, newBranchLayout, false);

      } catch (BranchLayoutException ignored) {}

      project.getService(GraphTableProvider.class).getGraphTable().queueRepositoryUpdateAndModelRefresh();
    }

  }

  @UIThreadUnsafe
  private static @Nullable GithubApiRequestExecutor getRequestExecutor(Project project) {
    val account = getGithubAccount();
    if (account == null) {
      return null;
    }
    String token = GHCompatibilityUtil.getOrRequestToken(account, project);
    if (token == null) {
      return null;
    }
    val factory = GithubApiRequestExecutor.Factory.getInstance();
    try { //Remove when min version >= 2022.3
      val method = factory.getClass().getMethod("create", String.class);
      return (GithubApiRequestExecutor) method.invoke(factory, token);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      return null;
    }
  }

  @UIThreadUnsafe
  private static @Nullable GHRepositoryCoordinates getRepositoryCoordinates(Project project) {
    val gitRepositoryProvider = project.getService(SelectedGitRepositoryProvider.class);

    val account = getGithubAccount();
    if (account == null) {
      return null;
    }

    val selectedGitRepository = gitRepositoryProvider.getSelectedGitRepository();
    if (selectedGitRepository == null) {
      return null;
    }
    val gitRemote = selectedGitRepository.getRemotes().iterator().next();
    val url = gitRemote.getFirstUrl();
    if (url == null) {
      return null;
    }
    val repositoryPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(url);
    if (repositoryPath == null) {
      return null;
    }
    return new GHRepositoryCoordinates(account.getServer(), repositoryPath);
  }

  @UIThreadUnsafe
  private static @Nullable GithubAccount getGithubAccount() {
    val accounts = List.ofAll(GHAccountsUtil.getAccounts());
    return accounts.headOption().getOrNull();
  }
}
