package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.actions.base.BaseResetToRemoteAction.VCS_NOTIFIER_TITLE;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.RESET;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;

import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitMacheteBundle.class, GitVfsUtils.class})
@CustomLog
public class ResetCurrentToRemoteBackgroundable extends Task.Backgroundable {

  public LambdaLogger log() {
    return LOG;
  }

  private final String localBranchName;

  private final String remoteTrackingBranchName;

  private final GitRepository gitRepository;

  public ResetCurrentToRemoteBackgroundable(Project project, String title,
      boolean canBeCancelled,
      String localBranchName, String remoteTrackingBranchName, GitRepository gitRepository) {
    super(project, title, canBeCancelled);
    this.localBranchName = localBranchName;
    this.remoteTrackingBranchName = remoteTrackingBranchName;
    this.gitRepository = gitRepository;
  }

  @Override
  @UIThreadUnsafe
  public void run(ProgressIndicator indicator) {
    if (myProject != null && localBranchName != null && remoteTrackingBranchName != null) {

      log().debug(() -> "Resetting '${localBranchName}' to '${remoteTrackingBranchName}'");

      try (AccessToken ignored = DvcsUtil.workingTreeChangeStarted(myProject,
          getString("action.GitMachete.BaseResetToRemoteAction.task-title"))) {
        val resetHandler = new GitLineHandler(myProject, gitRepository.getRoot(), GitCommand.RESET);
        resetHandler.addParameters("--keep");
        resetHandler.addParameters(remoteTrackingBranchName);
        resetHandler.endOptions();

        val localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(
            gitRepository.getRoot(), RESET);
        resetHandler.addLineListener(localChangesDetector);

        val result = Git.getInstance().runCommand(resetHandler);

        if (result.success()) {
          VcsNotifier.getInstance(myProject).notifySuccess( /* displayId */ null,
              /* title */ "",
              getString("action.GitMachete.BaseResetToRemoteAction.notification.title.reset-success.HTML")
                  .fmt(localBranchName));
          log().debug(() -> "Branch '${localBranchName}' has been reset to '${remoteTrackingBranchName}");

        } else if (localChangesDetector.wasMessageDetected()) {
          LocalChangesWouldBeOverwrittenHelper.showErrorNotification(myProject,
              /* displayId */ null,
              gitRepository.getRoot(),
              /* operationName */ "Reset",
              localChangesDetector.getRelativeFilePaths());

        } else {
          log().error(result.getErrorOutputAsJoinedString());
          VcsNotifier.getInstance(myProject).notifyError(/* displayId */ null, VCS_NOTIFIER_TITLE,
              result.getErrorOutputAsHtmlString());
        }

        val repositoryRoot = gitRepository.getRootDirectory();
        GitRepositoryManager.getInstance(myProject).updateRepository(repositoryRoot);
        VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ true, /* reloadChildren */ false, repositoryRoot);
      }
    }
  }

}
