package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.createRefspec;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

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
  protected @I18nFormat({}) String getOperationName() {
    return getString("action.GitMachete.PullCurrentBranchFastForwardOnlyBackgroundable.operation-name");
  }

  @Override
  protected String getTargetBranchName() {
    return remoteBranch.getName();
  }

  @Override
  @UIThreadUnsafe
  protected @Nullable GitLineHandler createGitLineHandler() {
    val handler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.PULL);
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
    val remoteBranchFullNameAsLocalBranchOnRemote = remoteBranch.getFullNameAsLocalBranchOnRemote();
    val remoteBranchFullName = remoteBranch.getFullName();
    // This strategy is used to fetch branch from remote repository to remote branch in our repository.
    String refspec = createRefspec(remoteBranchFullNameAsLocalBranchOnRemote,
        remoteBranchFullName, /* allowNonFastForward */ true);
    handler.addParameters(refspec);
    // Updating the current local branch in our repository to the commit pointed by the just-fetched remote branch,
    // in turn, will happen fast-forward-only thanks to `--ff-only` flag.

    return handler;
  }
}
