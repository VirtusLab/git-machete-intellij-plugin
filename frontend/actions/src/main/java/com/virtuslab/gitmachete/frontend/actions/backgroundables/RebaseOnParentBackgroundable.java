package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.branch.GitRebaseParams;
import git4idea.commands.Git;
import git4idea.config.GitVersion;
import git4idea.rebase.GitRebaseOption;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;
import git4idea.util.GitFreezingProcess;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.GitMacheteMissingForkPointException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public class RebaseOnParentBackgroundable extends SideEffectingBackgroundable {

  private static final String NL = System.lineSeparator();

  private final GitRepository gitRepository;
  private final IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;
  private final INonRootManagedBranchSnapshot branchToRebase;
  private final boolean shouldExplicitlyCheckout;

  public RebaseOnParentBackgroundable(String title, GitRepository gitRepository,
      IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot,
      INonRootManagedBranchSnapshot branchToRebase,
      boolean shouldExplicitlyCheckout) {
    super(gitRepository.getProject(), title, /* name */ "rebase");

    this.gitRepository = gitRepository;
    this.branchToRebase = branchToRebase;
    this.gitMacheteRepositorySnapshot = gitMacheteRepositorySnapshot;
    this.shouldExplicitlyCheckout = shouldExplicitlyCheckout;
    LOG.debug(() -> "Entering: gitRepository = ${gitRepository}, branchToRebase = ${branchToRebase}");
  }

  @UIThreadUnsafe
  private @Nullable GitRebaseOption getAvailableGitRebaseOptions(String optionText, GitVersion gitVersion) {
    val maybeEmptyDropEntry = Arrays.stream(GitRebaseOption.values())
        .filter(entry -> entry.getOption(gitVersion).equals(optionText)).findFirst();

    return maybeEmptyDropEntry.orElse(null);
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
  public void doRun(ProgressIndicator indicator) {
    IGitRebaseParameters gitRebaseParameters;
    try {
      gitRebaseParameters = branchToRebase.getParametersForRebaseOntoParent();
      LOG.debug(() -> "Queuing the machete-pre-rebase hook and the rebase background task for branch " +
          "'${branchToRebase.getName()}");
    } catch (GitMacheteMissingForkPointException e) {
      val message = e.getMessage() == null ? "Unable to get rebase parameters." : e.getMessage();
      LOG.error(message);
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-fail"), message);

      return;
    }

    final AtomicReference<Try<@Nullable IExecutionResult>> wrapper = new AtomicReference<>(
        Try.success(null));
    new GitFreezingProcess(project, getTitle(), () -> {
      LOG.info("Executing machete-pre-rebase hooks");
      Try<@Nullable IExecutionResult> hookResult = Try
          .of(() -> gitMacheteRepositorySnapshot.executeMachetePreRebaseHookIfPresent(gitRebaseParameters));
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
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-abort"),
          message);
      return;
    }

    val executionResult = hookResult.get();
    if (executionResult != null && executionResult.getExitCode() != 0) {
      val message = "machete-pre-rebase hooks refused to rebase (exit code ${executionResult.getExitCode()})";
      LOG.error(message);
      val stdoutOption = executionResult.getStdout();
      val stderrOption = executionResult.getStderr();
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          getString("action.GitMachete.BaseSyncToParentByRebaseAction.notification.title.rebase-abort"), message
              + (!stdoutOption.isBlank() ? NL + "stdout:" + NL + stdoutOption : "")
              + (!stderrOption.isBlank() ? NL + "stderr:" + NL + stderrOption : ""));
      return;
    }

    val params = getIdeaRebaseParamsOf(gitRepository, gitRebaseParameters);
    LOG.info("Rebasing '${gitRebaseParameters.getCurrentBranch().getName()}' branch " +
        "until ${gitRebaseParameters.getForkPointCommit().getHash()} commit " +
        "onto ${gitRebaseParameters.getNewBaseBranch().getName()}");

    /*
     * Git4Idea ({@link git4idea.rebase.GitRebaseUtils#rebase}) does not allow rebasing in detached head state. However, it is
     * possible with Git (performing checkout implicitly) and should be allowed in the case of "Checkout and Rebase Onto Parent"
     * Action. To pass the git4idea check in such a case, we checkout the branch explicitly and then perform the actual rebase.
     */
    if (shouldExplicitlyCheckout) {
      val uiHandler = new GitBranchUiHandlerImpl(project, indicator);
      new GitBranchWorker(project, Git.getInstance(), uiHandler)
          .checkout(/* reference */ gitRebaseParameters.getCurrentBranch().getName(), /* detach */ false,
              Collections.singletonList(gitRepository));
    }
    GitRebaseUtils.rebase(project, Collections.singletonList(gitRepository), params, indicator);
  }
}
