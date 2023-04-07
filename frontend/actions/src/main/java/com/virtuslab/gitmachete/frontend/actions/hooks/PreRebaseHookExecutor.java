package com.virtuslab.gitmachete.frontend.actions.hooks;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import git4idea.repo.GitRepository;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;

import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public final class PreRebaseHookExecutor extends BaseGit4IdeaHookExecutor {
  private static final int EXECUTION_TIMEOUT_SECONDS = 10;

  @UIThreadUnsafe
  public PreRebaseHookExecutor(GitRepository gitRepository) {
    super("machete-pre-rebase", gitRepository);
  }

  /**
   * @param gitRebaseParameters git rebase parameters
   * @return true if the rebase flow can be continued, false if an error happened and the rebase flow should be aborted
   */
  @UIThreadUnsafe
  public boolean executeHookFor(IGitRebaseParameters gitRebaseParameters) {
    String failureNotificationTitle = getString(
        "action.GitMachete.RebaseOnParentBackgroundable.notification.title.rebase-abort");

    return executeGit4IdeaHook(failureNotificationTitle, EXECUTION_TIMEOUT_SECONDS,
        gitRebaseParameters.getNewBaseBranch().getFullName(),
        gitRebaseParameters.getForkPointCommit().getHash(),
        gitRebaseParameters.getCurrentBranch().getName());
  }

  @Override
  protected LambdaLogger log() {
    return LOG;
  }
}
