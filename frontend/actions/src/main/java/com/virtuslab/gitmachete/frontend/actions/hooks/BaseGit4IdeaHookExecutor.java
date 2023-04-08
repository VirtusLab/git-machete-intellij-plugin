package com.virtuslab.gitmachete.frontend.actions.hooks;

import java.util.concurrent.atomic.AtomicReference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import git4idea.util.GitFreezingProcess;
import io.vavr.collection.HashMap;
import io.vavr.control.Try;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.hooks.BaseHookExecutor;
import com.virtuslab.gitmachete.backend.hooks.ExecutionResult;
import com.virtuslab.gitmachete.backend.hooks.OnExecutionTimeout;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitVfsUtils.class)
abstract class BaseGit4IdeaHookExecutor extends BaseHookExecutor {

  private final Project project;

  @UIThreadUnsafe
  BaseGit4IdeaHookExecutor(String name, GitRepository gitRepository) {
    super(name, gitRepository.getRootDirectoryPath(), gitRepository.getMainGitDirectoryPath(),
        getGitConfigCoreHooksPath(gitRepository));

    this.project = gitRepository.getProject();
  }

  @UIThreadUnsafe
  private static @Nullable String getGitConfigCoreHooksPath(GitRepository gitRepository) {
    try {
      return GitConfigUtil.getValue(gitRepository.getProject(), gitRepository.getRoot(), "core.hooksPath");
    } catch (VcsException e) {
      return null;
    }
  }

  /**
   * @return true if the flow can be continued (including when the hook is missing or non-executable),
   *         false if an error happened and the flow should be aborted
   */
  @UIThreadUnsafe
  protected boolean executeGit4IdeaHook(String failureNotificationTitle, int timeoutSeconds, String... args) {

    AtomicReference<Try<@Nullable ExecutionResult>> wrapper = new AtomicReference<>(
        Try.success(null));

    // This operation title is solely used for error message ("Local changes are not available until ... is finished")
    // that is displayed when a VCS operation (like commit) is attempted while git is frozen.
    val operationTitle = "${name} hook";
    new GitFreezingProcess(project, operationTitle, () -> {
      log().info("Executing ${name} hook");
      Try<@Nullable ExecutionResult> tryHookResult = Try
          .of(() -> executeHook(timeoutSeconds, OnExecutionTimeout.THROW_EXCEPTION, /* environment */ HashMap.empty(), args));
      wrapper.set(tryHookResult);
    }).execute();
    Try<@Nullable ExecutionResult> tryExecutionResult = wrapper.get();
    assert tryExecutionResult != null : "tryExecutionResult should never be null";

    if (tryExecutionResult.isFailure()) {
      val e = tryExecutionResult.getCause();
      val cause = e.getCause();
      val message = "${name} hook failed with an exception:${NL}${cause != null ? cause.getMessage() : e.getMessage()}";
      log().error(message);
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null, failureNotificationTitle, message);
      return false;
    }

    val executionResult = tryExecutionResult.get();
    if (executionResult != null && executionResult.getExitCode() != 0) {
      val message = "${name} hook did not complete successfully (exit code ${executionResult.getExitCode()})";
      log().error(message);
      val stdout = executionResult.getStdout();
      val stderr = executionResult.getStderr();
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          failureNotificationTitle, message
              + (!stdout.isBlank() ? NL + "stdout:" + NL + stdout : "")
              + (!stderr.isBlank() ? NL + "stderr:" + NL + stderr : ""));
      return false;
    }

    return true;
  }
}
