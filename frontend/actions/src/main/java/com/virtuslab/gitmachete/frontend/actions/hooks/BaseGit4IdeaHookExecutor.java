package com.virtuslab.gitmachete.frontend.actions.hooks;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.HashMap;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.hooks.BaseHookExecutor;
import com.virtuslab.gitmachete.backend.hooks.ExecutionResult;
import com.virtuslab.gitmachete.backend.hooks.OnExecutionTimeout;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitVfsUtils.class)
public abstract class BaseGit4IdeaHookExecutor extends BaseHookExecutor {

  private final Project project;

  @UIThreadUnsafe
  public BaseGit4IdeaHookExecutor(String name, GitRepository gitRepository) {
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

  @UIThreadUnsafe
  protected boolean executeGit4IdeaHook(String failureNotificationTitle, int timeoutSeconds, String... args) {
    ExecutionResult executionResult;
    try {
      executionResult = super.executeHook(timeoutSeconds, OnExecutionTimeout.THROW_EXCEPTION, /* environment */ HashMap.empty(),
          args);
    } catch (GitMacheteException e) {
      val cause = e.getCause();
      val message = "${name} hook failed with an exception:${NL}${cause != null ? cause.getMessage() : e.getMessage()}";
      log().error(message);
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null, failureNotificationTitle, message);
      return false;
    }

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
