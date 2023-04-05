package com.virtuslab.gitmachete.backend.impl.hooks;

import io.vavr.collection.HashMap;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.hooks.ExecutionResult;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public final class PreRebaseHookExecutor extends BaseHookExecutor {
  private static final int EXECUTION_TIMEOUT_SECONDS = 10;

  @UIThreadUnsafe
  public PreRebaseHookExecutor(IGitCoreRepository gitCoreRepository) {
    super("machete-pre-rebase", gitCoreRepository);
  }

  /**
   * @param gitRebaseParameters git rebase parameters
   * @return an exit code (possibly non-zero) when the hook has been successfully executed,
   *         or null when the hook has not been executed (because it's absent or non-executable)
   * @throws GitMacheteException when a timeout or I/O exception occurs
   */
  public @Nullable ExecutionResult executeHookFor(IGitRebaseParameters gitRebaseParameters) throws GitMacheteException {
    return executeHook(EXECUTION_TIMEOUT_SECONDS, OnExecutionTimeout.THROW,
        /* environment */ HashMap.empty(),
        gitRebaseParameters.getNewBaseBranch().getFullName(),
        gitRebaseParameters.getForkPointCommit().getHash(),
        gitRebaseParameters.getCurrentBranch().getName());
  }

  @Override
  protected LambdaLogger log() {
    return LOG;
  }
}
