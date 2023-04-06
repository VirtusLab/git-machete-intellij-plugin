package com.virtuslab.gitmachete.frontend.actions.hooks;

import com.intellij.openapi.vcs.VcsException;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.hooks.BaseHookExecutor;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitVfsUtils.class)
public abstract class BaseGit4IdeaHookExecutor extends BaseHookExecutor {

  @UIThreadUnsafe
  public BaseGit4IdeaHookExecutor(String name, GitRepository gitRepository) {
    super(name, gitRepository.getRootDirectoryPath(), gitRepository.getMainGitDirectoryPath(),
        getGitConfigCoreHooksPath(gitRepository));
  }

  @UIThreadUnsafe
  private @Nullable static String getGitConfigCoreHooksPath(GitRepository gitRepository) {
    try {
      return GitConfigUtil.getValue(gitRepository.getProject(), gitRepository.getRoot(), "core.hooksPath");
    } catch (VcsException e) {
      return null;
    }
  }
}
