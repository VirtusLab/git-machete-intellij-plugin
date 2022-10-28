package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.branch.GitRebaseParams;
import git4idea.config.GitVersion;
import git4idea.rebase.GitRebaseOption;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;
import git4idea.util.GitFreezingProcess;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;
import com.virtuslab.gitmachete.frontend.actions.contextmenu.CheckoutSelectedAction;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({Arrays.class})
@CustomLog
public class RebaseOnParentBackgroundable extends Task.Backgroundable {

  private final GitRepository gitRepository;

  private static final String NL = System.lineSeparator();
  private final IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  @Nullable
  private final IGitRebaseParameters gitRebaseParameters;

  private final INonRootManagedBranchSnapshot branchToRebase;

  private final Try<IGitRebaseParameters> tryGitRebaseParameters;

  private final boolean shouldExplicitlyCheckout;

  public RebaseOnParentBackgroundable(Project project, String title, GitRepository gitRepository,
      IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot,
      INonRootManagedBranchSnapshot branchToRebase,
      boolean shouldExplicitlyCheckout) {
    super(project, title);
    this.gitRepository = gitRepository;
    this.branchToRebase = branchToRebase;
    this.gitMacheteRepositorySnapshot = gitMacheteRepositorySnapshot;
    this.shouldExplicitlyCheckout = shouldExplicitlyCheckout;
    LOG.debug(() -> "Entering: project = ${project}, gitRepository = ${gitRepository}, branchToRebase = ${branchToRebase}");

    tryGitRebaseParameters = Try.of(branchToRebase::getParametersForRebaseOntoParent);

    if (tryGitRebaseParameters.isFailure()) {
      val e = tryGitRebaseParameters.getCause();
      val message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
      LOG.error(message);
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-fail"), message);
      gitRebaseParameters = null;
    } else {
      gitRebaseParameters = tryGitRebaseParameters.get();
      LOG.debug(() -> "Queuing machete-pre-rebase hooks background task for '${branchToRebase.getName()}' branch");
    }

  }

  public LambdaLogger log() {
    return LOG;
  }

  @UIThreadUnsafe
  private @Nullable GitRebaseOption getAvailableGitRebaseOptions(String optionText, GitVersion gitVersion) {
    val maybeEmptyDropEntry = GitRebaseOption.values().stream()
        .filter(entry -> entry.getOption(gitVersion).equals(optionText))
        .findFirst();

    return Option.ofOptional(maybeEmptyDropEntry).getOrNull();
  }

  @UIThreadUnsafe
  private GitRebaseParams getIdeaRebaseParamsOf(GitRepository repository, IGitRebaseParameters gitRebaseParams) {
    val gitVersion = repository.getVcs().getVersion();
    val currentBranchName = gitRebaseParams.getCurrentBranch().getName();
    val newBaseBranchFullName = gitRebaseParams.getNewBaseBranch().getFullName();
    val forkPointCommitHash = gitRebaseParams.getForkPointCommit().getHash();

    // TODO (#1114): remove the mechanism for checking the availability of "--empty=drop"
    val options = kotlin.collections.SetsKt.hashSetOf(GitRebaseOption.INTERACTIVE);

    val gitRebaseOption = getAvailableGitRebaseOptions("--empty=drop", gitVersion);
    if (gitRebaseOption != null) {
      options.add(gitRebaseOption);
    }

    return new GitRebaseParams(gitVersion, currentBranchName, newBaseBranchFullName,
        /* upstream */ forkPointCommitHash, /* selectedOptions */ options, GitRebaseParams.AutoSquashOption.DEFAULT,
        /* editorHandler */ null);
  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    if (myProject != null && gitRebaseParameters != null) {
      val gitRebaseParams = gitRebaseParameters;

      final AtomicReference<Try<@Nullable IExecutionResult>> wrapper = new AtomicReference<>(Try.success(null));
      new GitFreezingProcess(myProject, getTitle(), () -> {
        LOG.info("Executing machete-pre-rebase hooks");
        Try<@Nullable IExecutionResult> hookResult = Try
            .of(() -> gitMacheteRepositorySnapshot.executeMachetePreRebaseHookIfPresent(gitRebaseParams));
        wrapper.set(hookResult);
      }).execute();
      Try<@Nullable IExecutionResult> hookResult = wrapper.get();
      if (hookResult == null) {
        // Not really possible, it's here just to calm down Checker Framework.
        return;
      }

      if (hookResult.isFailure()) {
        val message = "machete-pre-rebase hooks refused to rebase ${NL}error: ${hookResult.getCause().getMessage()}";
        LOG.error(message);
        VcsNotifier.getInstance(myProject).notifyError(/* displayId */ null,
            getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-abort"),
            message);
        return;
      }

      val maybeExecutionResult = hookResult.get();
      if (maybeExecutionResult != null && maybeExecutionResult.getExitCode() != 0) {
        val message = "machete-pre-rebase hooks refused to rebase (exit code ${maybeExecutionResult.getExitCode()})";
        LOG.error(message);
        val executionResult = maybeExecutionResult;
        val stdoutOption = executionResult.getStdout();
        val stderrOption = executionResult.getStderr();
        VcsNotifier.getInstance(myProject).notifyError(/* displayId */ null,
            getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-abort"), message
                + (!stdoutOption.isBlank() ? NL + "stdout:" + NL + stdoutOption : "")
                + (!stderrOption.isBlank() ? NL + "stderr:" + NL + stderrOption : ""));
        return;
      }
      /*
       * IMPORTANT: TO BE REVIEWED here there used to be an extra layer of concurrency through the wrapping of the below code in
       * a Task.Backgroundable. But that was removed, since this was appearing unnecessary and causing the rebase thread to
       * potentially outlive the run thread of RebaseOnParentBackgroundable which could cause out of sync continuation of the
       * traverse action. In other words, this could have caused the traverse to move on to sync to remote or the next branch
       * before the end of the rebasing of this branch.
       */
      val params = getIdeaRebaseParamsOf(gitRepository, gitRebaseParams);
      LOG.info("Rebasing '${gitRebaseParameters.getCurrentBranch().getName()}' branch " +
          "until ${gitRebaseParameters.getForkPointCommit().getHash()} commit " +
          "onto ${gitRebaseParameters.getNewBaseBranch().getName()}");

      /*
       * Git4Idea ({@link git4idea.rebase.GitRebaseUtils#rebase}) does not allow to rebase in detached head state. However, it
       * is possible with Git (performing checkout implicitly) and should be allowed in the case of
       * "Checkout and Rebase Onto Parent" Action. To pass the git4idea check in such a case we checkout the branch explicitly
       * and then perform the actual rebase.
       */
      if (shouldExplicitlyCheckout) {
        CheckoutSelectedAction.doCheckout(
            myProject, indicator, gitRebaseParameters.getCurrentBranch().getName(), gitRepository);
      }
      GitRebaseUtils.rebase(myProject, Collections.singletonList(gitRepository), params, indicator);
    }

  }

}
