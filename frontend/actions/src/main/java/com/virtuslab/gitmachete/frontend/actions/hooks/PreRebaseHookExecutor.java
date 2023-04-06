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

  @UIThreadUnsafe
  public boolean executeHookFor(IGitRebaseParameters gitRebaseParameters) {
    String failureNotificationTitle = getString(
        "action.GitMachete.PreRebaseHookExecutor.notification.title.abort");

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
