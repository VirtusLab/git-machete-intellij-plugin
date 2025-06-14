package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.actions.base.BaseResetToRemoteAction.VCS_NOTIFIER_TITLE;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static git4idea.GitNotificationIdsHolder.LOCAL_CHANGES_DETECTED;
import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.RESET;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;

import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
@ExtensionMethod({GitMacheteBundle.class})
// For some reason, using `@ExtensionMethod({GitVfsUtils.class})` on this class
// leads to weird Checker Framework errors :/
public class ResetCurrentToRemoteBackgroundable extends SideEffectingBackgroundable {

  private final String localBranchName;
  private final String remoteTrackingBranchName;
  private final GitRepository gitRepository;

  public ResetCurrentToRemoteBackgroundable(String localBranchName, String remoteTrackingBranchName,
      GitRepository gitRepository) {
    super(gitRepository.getProject(), getNonHtmlString("action.GitMachete.ResetCurrentToRemoteBackgroundable.task-title"),
        "reset");
    this.localBranchName = localBranchName;
    this.remoteTrackingBranchName = remoteTrackingBranchName;
    this.gitRepository = gitRepository;
  }

  @Override
  @UIThreadUnsafe
  public void doRun(ProgressIndicator indicator) {
    if (localBranchName != null && remoteTrackingBranchName != null) {

      LOG.debug(() -> "Resetting '${localBranchName}' to '${remoteTrackingBranchName}'");

      try (AccessToken ignored = DvcsUtil.workingTreeChangeStarted(project,
          getString("action.GitMachete.ResetCurrentToRemoteBackgroundable.task-title"))) {
        val resetHandler = new GitLineHandler(project, gitRepository.getRoot(), GitCommand.RESET);
        resetHandler.addParameters("--keep");
        resetHandler.addParameters(remoteTrackingBranchName);
        resetHandler.endOptions();

        val localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(gitRepository.getRoot(), RESET);
        resetHandler.addLineListener(localChangesDetector);

        val result = Git.getInstance().runCommand(resetHandler);

        if (result.success()) {
          VcsNotifier.getInstance(project).notifySuccess( /* displayId */ null,
              /* title */ "",
              getString("action.GitMachete.BaseResetToRemoteAction.notification.title.reset-success.HTML")
                  .fmt(localBranchName));
          LOG.debug(() -> "Branch '${localBranchName}' has been reset to '${remoteTrackingBranchName}");

        } else if (localChangesDetector.wasMessageDetected()) {
          LocalChangesWouldBeOverwrittenHelper.showErrorNotification(project,
              LOCAL_CHANGES_DETECTED,
              gitRepository.getRoot(),
              /* operationName */ "Reset",
              localChangesDetector.getRelativeFilePaths());

        } else {
          LOG.error(result.getErrorOutputAsJoinedString());
          VcsNotifier.getInstance(project).notifyError(/* displayId */ null, VCS_NOTIFIER_TITLE,
              result.getErrorOutputAsHtmlString());
        }

        val repositoryRoot = gitRepository.getRoot();
        GitRepositoryManager.getInstance(project).updateRepository(repositoryRoot);
        VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ true, /* reloadChildren */ false, repositoryRoot);
      }
    }
  }

}
