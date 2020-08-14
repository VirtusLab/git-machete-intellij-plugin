package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;

@CustomLog
public class PullCurrentBranchFastForwardOnlyBackgroundable extends GitCommandUpdatingCurrentBranchBackgroundable {

  private final IRemoteTrackingBranchReference remoteBranch;

  public PullCurrentBranchFastForwardOnlyBackgroundable(
      Project project,
      GitRepository gitRepository,
      String taskTitle,
      IRemoteTrackingBranchReference remoteBranch) {
    super(project, gitRepository, taskTitle);
    this.remoteBranch = remoteBranch;
  }

  @Override
  protected String getOperationName() {
    return "Pull";
  }

  @Override
  protected String getTargetBranchName() {
    return remoteBranch.getName();
  }

  @Override
  protected @Nullable GitLineHandler createGitLineHandler() {
    var handler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.PULL);
    String remoteName = remoteBranch.getRemoteName();
    GitRemote remote = GitUtil.findRemoteByName(gitRepository, remoteName);
    if (remote == null) {
      // This is generally NOT expected, the task should never be triggered
      // for an invalid remote in the first place.
      LOG.warn("Remote '${remoteName}' does not exist");
      return null;
    }
    handler.setUrls(remote.getUrls());
    handler.addParameters("--ff-only");
    handler.addParameters(remote.getName());
    var remoteBranchFullNameAsLocalBranchOnRemote = remoteBranch.getFullNameAsLocalBranchOnRemote();
    var remoteBranchFullName = remoteBranch.getFullName();
    // Note the '+' sign preceding the refspec. It permits non-fast-forward updates.
    // This strategy is used to fetch branch from remote repository to remote branch in our repository.
    handler.addParameters("+${remoteBranchFullNameAsLocalBranchOnRemote}:${remoteBranchFullName}");
    // Updating the current local branch in our repository to the commit pointed by the just-fetched remote branch,
    // in turn, will happen fast-forward-only thanks to `--ff-only` flag.

    return handler;
  }
}
