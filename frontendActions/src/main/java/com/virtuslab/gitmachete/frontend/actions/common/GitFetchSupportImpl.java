package com.virtuslab.gitmachete.frontend.actions.common;

import static com.intellij.openapi.vcs.VcsNotifier.STANDARD_NOTIFICATION;
import static git4idea.GitUtil.findRemoteByName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.intellij.dvcs.MultiMessage;
import com.intellij.dvcs.MultiRootMessage;
import com.intellij.internal.statistic.IdeActivity;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.util.concurrency.AppExecutorUtil;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitAuthenticationGate;
import git4idea.commands.GitAuthenticationListener;
import git4idea.commands.GitImpl;
import git4idea.commands.GitRestrictingAuthenticationGate;
import git4idea.config.GitConfigUtil;
import git4idea.fetch.GitFetchResult;
import git4idea.fetch.GitFetchSupport;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BasePullBranchAction;
import com.virtuslab.gitmachete.frontend.actions.toolbar.FetchAllRemotesAction;

/**
 * @deprecated This implementation is a workaround to provide features missing in pre-2020.1 versions.
 * The main benefit of it is specific branch update ({@link GitFetchSupportImpl#fetch(GitRepository, GitRemote, String)}).
 * We need this class for {@link FetchAllRemotesAction} and {@link BasePullBranchAction}.
 */
@CustomLog
@Deprecated
@RequiredArgsConstructor
public final class GitFetchSupportImpl implements GitFetchSupport {

  private final Project project;
  private final ProgressManager progressManager = ProgressManager.getInstance();

  private final AtomicInteger fetchRequestCounter = new AtomicInteger(0);

  public static GitFetchSupportImpl fetchSupport(Project project) {
    return ServiceManager.getService(project, GitFetchSupportImpl.class);
  }

  public boolean isFetchRunning() {
    return fetchRequestCounter.get() > 0;
  }

  @Override
  public FetchResultImpl fetchDefaultRemote(Collection<GitRepository> repositories) {
    var remotesToFetch = List.ofAll(repositories).map(repo -> Tuple.of(repo, getDefaultRemoteToFetch(repo)))
        .filter(repoAndRemote -> {
          if (repoAndRemote._2() != null) {
            return true;
          } else {
            LOG.info("No remote to fetch found in ${repoAndRemote._1()}");
            return false;
          }

        }).map(repoAndRemote -> new RemoteRefCoordinates(repoAndRemote._1(), repoAndRemote._2(), /* refspec */ null));
    return fetch(remotesToFetch);
  }

  @Override
  public FetchResultImpl fetchAllRemotes(Collection<GitRepository> repositories) {
    var remotesToFetch = List.ofAll(repositories).map(repo -> Tuple.of(repo, List.ofAll(repo.getRemotes())))
        .filter(repoAndRemote -> {
          if (repoAndRemote._2().nonEmpty()) {
            return true;
          } else {
            LOG.info("No remote to fetch found in ${repoAndRemote._1()}");
            return false;
          }

        })
        .flatMap(repoAndRemote -> repoAndRemote._2()
            .map(remote -> new RemoteRefCoordinates(repoAndRemote._1(), remote, /* refspec */ null)));
    return fetch(remotesToFetch);
  }

  public FetchResultImpl fetch(GitRepository repository, GitRemote remote, String refspec) {
    return fetch(List.of(new RemoteRefCoordinates(repository, remote, refspec)));
  }

  @Override
  public FetchResultImpl fetch(GitRepository repository, GitRemote remote) {
    return fetch(List.of(new RemoteRefCoordinates(repository, remote, /* refspec */ null)));
  }

  public FetchResultImpl fetch(List<RemoteRefCoordinates> arguments) {
    try {
      fetchRequestCounter.incrementAndGet();
      return withIndicator(() -> {
        var activity = IdeActivity.started(project, /* group */ "vcs", /* activityName */ "fetch");

        var tasks = fetchInParallel(arguments);
        var results = waitForFetchTasks(tasks);

        var mergedResults = new java.util.HashMap<GitRepository, RepoResult>();
        for (var result : results) {
          mergedResults.compute(result.repository, (repo, res) -> mergeRepoResults(res, result));
        }

        activity.finished();

        return new FetchResultImpl(project, VcsNotifier.getInstance(project), HashMap.ofAll(mergedResults));
      });
    } finally {
      fetchRequestCounter.decrementAndGet();
    }
  }

  private List<FetchTask> fetchInParallel(List<RemoteRefCoordinates> remotes) {
    var maxThreads = getMaxThreads(remotes.map(r -> r.repository), remotes.size());
    LOG.debug("Fetching ${remotes} using ${maxThreads} threads");
    var executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(/* name */ "GitFetch Pool", maxThreads);
    var commonIndicator = Option.of(progressManager.getProgressIndicator()).getOrElse(new EmptyProgressIndicator());
    var authenticationGate = new GitRestrictingAuthenticationGate();

    return remotes.map(remoteRefCoordinates -> {
      LOG.debug("Fetching ${remoteRefCoordinates.remote} in ${remoteRefCoordinates.repository}");
      Future<SingleRemoteResult> future = executor.submit(() -> {
        commonIndicator.checkCanceled();

        var singleRemoteResult = Lazy.of(() -> doFetch(
            remoteRefCoordinates.repository,
            remoteRefCoordinates.remote,
            remoteRefCoordinates.refspec,
            authenticationGate));

        ProgressManager.getInstance().executeProcessUnderProgress(() -> {
          commonIndicator.checkCanceled();
          singleRemoteResult.get();
        }, commonIndicator);

        return singleRemoteResult.get();
      });

      return new FetchTask(remoteRefCoordinates.repository, remoteRefCoordinates.remote, future);
    });
  }

  private int getMaxThreads(List<GitRepository> repositories, int numberOfRemotes) {
    return !isStoreCredentialsHelperUsed(repositories)
        ? Math.min(numberOfRemotes, Runtime.getRuntime().availableProcessors() * 2)
        : 1;
  }

  private boolean isStoreCredentialsHelperUsed(List<GitRepository> repositories) {
    for (var repo : repositories) {
      var option = Try.of(() -> Option.of(GitConfigUtil.getValue(project, repo.getRoot(), "credential.helper")))
          .getOrElse(Option.none());
      if (option.isDefined() && option.get().equalsIgnoreCase("store")) {
        return true;
      }
    }
    return false;
  }

  private List<SingleRemoteResult> waitForFetchTasks(List<FetchTask> tasks) {
    var results = new ArrayList<SingleRemoteResult>();
    for (var task : tasks) {
      try {
        results.add(task.future.get());
      } catch (CancellationException | InterruptedException e) {
        throw new ProcessCanceledException(e);
      } catch (ExecutionException e) {
        if (e.getCause() instanceof ProcessCanceledException) {
          throw (ProcessCanceledException) e.getCause();
        }
        String msg = Option.of(e.getCause())
            .flatMap(c -> Option.of(c.getMessage()))
            .getOrElse("Error");
        results.add(new SingleRemoteResult(task.repository, task.remote, msg, List.empty()));
        LOG.error("Task execution error: ${msg}");
      }
    }
    return List.ofAll(results);
  }

  private RepoResult mergeRepoResults(@Nullable RepoResult firstResult, SingleRemoteResult secondResult) {
    if (firstResult == null) {
      return new RepoResult(HashMap.of(secondResult.remote, secondResult));
    } else {
      var results = HashMap.of(secondResult.remote, secondResult).merge(firstResult.results);
      return new RepoResult(results);
    }
  }

  private <T> T withIndicator(Supplier<T> operation) {
    var indicator = progressManager.getProgressIndicator();
    var prevText = "";
    if (indicator != null) {
      prevText = indicator.getText();
      indicator.setText("Fetching...");
    }
    try {
      return operation.get();
    } finally {
      if (indicator != null) {
        indicator.setText(prevText);
      }
    }
  }

  @Override
  public @Nullable GitRemote getDefaultRemoteToFetch(GitRepository repository) {
    var remotes = repository.getRemotes();
    if (remotes.isEmpty()) {
      return null;
    } else if (remotes.size() == 1) {
      return remotes.iterator().next();
    } else {
      // this emulates behavior of the native `git fetch`:
      // if current branch doesn't give a hint, then return "origin"; if there is no "origin", don't guess and fail
      return Option.of(repository.getCurrentBranch())
          .flatMap(b -> Option.of(b.findTrackedBranch(repository)))
          .map(t -> t.getRemote())
          .flatMap(r -> Option.of(findRemoteByName(repository, GitRemote.ORIGIN)))
          .getOrNull();
    }
  }

  private SingleRemoteResult doFetch(GitRepository repository, GitRemote remote, @Nullable String refspec,
      GitAuthenticationGate authenticationGate) {
    var recurseSubmodules = "--recurse-submodules=no";

    // By default git fetch refuses to update the head which corresponds to the current branch.
    // This flag disables the check.
    var updateHeadOk = "--update-head-ok";

    List<String> params = refspec == null
        ? List.of(recurseSubmodules, updateHeadOk)
        : List.of(refspec, recurseSubmodules, updateHeadOk);

    GitImpl gitInstance = (GitImpl) Git.getInstance();
    var result = gitInstance.fetch(repository, remote, Collections.emptyList(), authenticationGate,
        params.toJavaArray(String[]::new));
    var pruned = List.ofAll(result.getOutput()).map(this::getPrunedRef).filter(r -> r.isEmpty());

    if (result.success()) {
      BackgroundTaskUtil.syncPublisher(repository.getProject(), GitAuthenticationListener.GIT_AUTHENTICATION_SUCCESS)
          .authenticationSucceeded(repository, remote);
      repository.update();
    }

    String error = result.success() ? null : result.getErrorOutputAsJoinedString();

    return new SingleRemoteResult(repository, remote, error, pruned);
  }

  private String getPrunedRef(String line) {
    var PRUNE_PATTERN = Pattern.compile("\\s*x\\s*\\[deleted\\].*->\\s*(\\S*)"); // x [deleted] (none) -> origin/branch
    var matcher = PRUNE_PATTERN.matcher(line);
    String result = "";
    if (matcher.matches()) {
      result = matcher.group(1);
    }
    assert result != null : "Matched group is null";
    return result;
  }

  @AllArgsConstructor
  private class RemoteRefCoordinates {
    private final GitRepository repository;
    private final GitRemote remote;
    private final @Nullable String refspec;
  }

  @AllArgsConstructor
  private class FetchTask {
    private final GitRepository repository;
    private final GitRemote remote;
    private final Future<SingleRemoteResult> future;
  }

  @AllArgsConstructor
  private class RepoResult {
    Map<GitRemote, SingleRemoteResult> results;

    boolean totallySuccessful() {
      return results.values().forAll(v -> v.success());
    }

    @Nullable
    String error() {
      var errorMessage = multiRemoteMessage(true);
      for (Tuple2<GitRemote, SingleRemoteResult> res : results) {
        String error = res._2().error;
        if (error != null) {
          errorMessage.append(res._1(), error);
        }
      }
      return errorMessage.asString();
    }

    String prunedRefs() {
      var prunedRefs = multiRemoteMessage(false);
      results.filter(r -> r._2().prunedRefs.nonEmpty())
          .forEach(r -> prunedRefs.append(r._1(), String.join(System.lineSeparator(), r._2().prunedRefs)));
      return prunedRefs.asString();
    }

    /*
     * For simplicity, remote and repository results are merged separately. It means that they are not merged, if two
     * repositories have two remotes, and then fetch succeeds for the first remote in both repos, and fails for the second
     * remote in both repos. Such cases are rare, and can be handled when actual problem is reported.
     */
    private MultiMessage<GitRemote> multiRemoteMessage(boolean remoteInPrefix) {
      return new MultiMessage<GitRemote>(
          results.keySet().toJavaSet(),
          GitRemote::getName,
          GitRemote::getName,
          remoteInPrefix,
          /* html */ true);
    }
  }

  @AllArgsConstructor
  private class SingleRemoteResult {
    private final GitRepository repository;
    private final GitRemote remote;
    private final @Nullable String error;
    private final List<String> prunedRefs;

    public boolean success() {
      return error == null;
    }
  }

  public final class FetchResultImpl implements GitFetchResult {
    private final Project project;
    private final VcsNotifier vcsNotifier;
    private final Map<GitRepository, RepoResult> results;
    private final boolean isFailed;

    private FetchResultImpl(Project project, VcsNotifier vcsNotifier, Map<GitRepository, RepoResult> results) {
      this.project = project;
      this.vcsNotifier = vcsNotifier;
      this.results = results;
      this.isFailed = results.values().find(v -> !v.totallySuccessful()).isDefined();
    }

    @Override
    public void showNotification() {
      doShowNotification();
    }

    @Override
    public boolean showNotificationIfFailed(String title) {
      if (isFailed) {
        doShowNotification(title);
      }
      return !isFailed;
    }

    @Override
    public boolean showNotificationIfFailed() {
      return showNotificationIfFailed("Fetch Failed");
    }

    private void doShowNotification(String failureTitle) {
      NotificationType type = isFailed ? NotificationType.ERROR : NotificationType.INFORMATION;
      var message = buildMessage(failureTitle);
      var notification = STANDARD_NOTIFICATION.createNotification(/* title */ "", message, type, /* listener */ null);
      vcsNotifier.notify(notification);
    }

    private void doShowNotification() {
      doShowNotification("Fetch Failed");
    }

    public void throwExceptionIfFailed() {
      // Actual (idea) implementations do throw here like we do in `FetchResultImpl.ourThrowExceptionIfFailed`.
      // This is achieved thanks to Kotlin which has no checked exception.
    }

    public void ourThrowExceptionIfFailed() throws VcsException {
      if (isFailed) {
        throw new VcsException(buildMessage());
      }
    }

    private String buildMessage(String failureTitle) {
      var roots = results.keySet().map(it -> it.getRoot()).collect(Collectors.toList());
      var errorMessage = new MultiRootMessage(project, roots, /* rootInPrefix */ true, /* html */ true);
      var prunedRefs = new MultiRootMessage(project, roots, /* rootInPrefix */ false, /* html */ true);
      var failed = results.toStream()
          .filter(e -> !e._2().totallySuccessful())
          .collect(HashMap.collector());

      for (Tuple2<GitRepository, RepoResult> fail : failed) {
        String error = fail._2().error();
        if (error != null) {
          errorMessage.append(fail._1().getRoot(), error);
        }
      }

      for (Tuple2<GitRepository, RepoResult> res : results) {
        prunedRefs.append(res._1().getRoot(), res._2().prunedRefs());
      }

      var mentionFailedRepos = failed.size() == roots.size() ? "" : GitUtil.mention(failed.keySet().toJavaSet());
      var title = !isFailed ? "<b>Fetch Successful</b>" : "<b>${failureTitle}</b>${mentionFailedRepos}";
      return title + prefixWithBr(errorMessage.asString()) + prefixWithBr(prunedRefs.asString());
    }

    private String buildMessage() {
      return buildMessage("Fetch Failed");
    }

    private String prefixWithBr(String text) {
      return text.isEmpty() ? "" : "<br/>${text}";
    }

  }
}
