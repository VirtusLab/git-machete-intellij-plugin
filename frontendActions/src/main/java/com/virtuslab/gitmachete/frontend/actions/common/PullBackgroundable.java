package com.virtuslab.gitmachete.frontend.actions.common;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.MERGE;
import static git4idea.update.GitUpdateSessionKt.getBodyForUpdateNotification;
import static git4idea.update.GitUpdateSessionKt.getTitleForUpdateNotification;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.history.Label;
import com.intellij.history.LocalHistory;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.intellij.openapi.vcs.update.AbstractCommonUpdateAction;
import com.intellij.openapi.vcs.update.ActionInfo;
import com.intellij.openapi.vcs.update.UpdateInfoTree;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.GuiUtils;
import com.intellij.vcs.ViewUpdateInfoNotification;
import git4idea.GitBranch;
import git4idea.GitRevisionNumber;
import git4idea.GitVcs;
import git4idea.branch.GitBranchPair;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector;
import git4idea.commands.GitUntrackedFilesOverwrittenByOperationDetector;
import git4idea.merge.MergeChangeCollector;
import git4idea.repo.GitRepository;
import git4idea.update.GitUpdateInfoAsLog;
import git4idea.update.GitUpdatedRanges;
import git4idea.util.GitUIUtil;
import git4idea.util.GitUntrackedFilesHelper;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import lombok.CustomLog;
import org.checkerframework.checker.nullness.qual.Nullable;

@CustomLog
public class PullBackgroundable extends Task.Backgroundable {

  private static final String OPERATION_NAME = "Pull";

  private final Project project;
  private final GitRepository gitRepository;
  private final GitLineHandler handler;
  private final String remoteBranchName;

  public PullBackgroundable(
      Project project,
      GitRepository gitRepository,
      GitLineHandler handler,
      String remoteBranchName) {
    super(project,
        /* taskTitle */ getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.task-title"),
        /* canBeCancelled */ true);
    this.project = project;
    this.gitRepository = gitRepository;
    this.handler = handler;
    this.remoteBranchName = remoteBranchName;
  }

  @Override
  public void run(ProgressIndicator indicator) {
    var localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(gitRepository.getRoot(), MERGE);
    var untrackedFilesDetector = new GitUntrackedFilesOverwrittenByOperationDetector(gitRepository.getRoot());

    handler.addLineListener(localChangesDetector);
    handler.addLineListener(untrackedFilesDetector);

    Label beforeLabel = LocalHistory.getInstance().putSystemLabel(project, /* name */ "Before update");

    GitUpdatedRanges updatedRanges = deriveGitUpdatedRanges(project, gitRepository, remoteBranchName);

    String beforeRevision = gitRepository.getCurrentRevision();
    try (AccessToken ignore = DvcsUtil.workingTreeChangeStarted(project, OPERATION_NAME)) {
      GitCommandResult result = Git.getInstance().runCommand(handler);

      if (beforeRevision != null) {
        GitRevisionNumber currentRev = new GitRevisionNumber(beforeRevision);
        handleResult(result,
            project,
            localChangesDetector,
            untrackedFilesDetector,
            gitRepository,
            currentRev,
            beforeLabel,
            updatedRanges);
      }
    }
  }

  private static @Nullable GitUpdatedRanges deriveGitUpdatedRanges(
      Project project,
      GitRepository gitRepository,
      String remoteBranchName) {
    GitUpdatedRanges updatedRanges = null;
    var currentBranch = gitRepository.getCurrentBranch();
    if (currentBranch != null) {
      GitBranch targetBranch = gitRepository.getBranches().findBranchByName(remoteBranchName);
      if (targetBranch != null) {
        GitBranchPair refPair = new GitBranchPair(currentBranch, targetBranch);
        updatedRanges = GitUpdatedRanges.calcInitialPositions(project,
            java.util.Collections.singletonMap(gitRepository, refPair));
      } else {
        LOG.warn("Couldn't find the branch with name '${remoteBranchName}'");
      }
    }
    return updatedRanges;
  }

  private static void handleResult(
      GitCommandResult result,
      Project project,
      GitLocalChangesWouldBeOverwrittenDetector localChangesDetector,
      GitUntrackedFilesOverwrittenByOperationDetector untrackedFilesDetector,
      GitRepository repository,
      GitRevisionNumber currentRev,
      Label beforeLabel,
      @Nullable GitUpdatedRanges updatedRanges) {
    VirtualFile root = repository.getRoot();
    if (result.success()) {
      VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ true, /* reloadChildren */ false, root);
      repository.update();
      if (updatedRanges != null &&
          AbstractCommonUpdateAction
              .showsCustomNotification(java.util.Collections.singletonList(GitVcs.getInstance(project)))) {
        var ranges = updatedRanges.calcCurrentPositions();
        GitUpdateInfoAsLog.NotificationData notificationData = new GitUpdateInfoAsLog(project, ranges)
            .calculateDataAndCreateLogTab();

        Notification notification;
        if (notificationData != null) {
          String title = getTitleForUpdateNotification(notificationData.getUpdatedFilesCount(),
              notificationData.getReceivedCommitsCount());
          String content = getBodyForUpdateNotification(notificationData.getFilteredCommitsCount());
          notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(title,
              content,
              INFORMATION,
              /* listener */ null);
          notification.addAction(NotificationAction.createSimple(getString(
              "action.GitMachete.PullBackgroundable.notification.message.view-commits"),
              notificationData.getViewCommitAction()));

        } else {
          // When the pull results with no commits, there is no git update info (as log).
          // Based on that we know that all files are up-to-date.
          notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
              getString("action.GitMachete.PullBackgroundable.notification.title.all-files-are-up-to-date"),
              /* content */ "", INFORMATION, /* listener */ null);
        }
        VcsNotifier.getInstance(project).notify(notification);

      } else {
        showUpdates(project, root, currentRev, beforeLabel);
      }

    } else if (localChangesDetector.wasMessageDetected()) {
      LocalChangesWouldBeOverwrittenHelper.showErrorNotification(project,
          repository.getRoot(),
          OPERATION_NAME,
          localChangesDetector.getRelativeFilePaths());

    } else if (untrackedFilesDetector.wasMessageDetected()) {
      GitUntrackedFilesHelper.notifyUntrackedFilesOverwrittenBy(project,
          root,
          untrackedFilesDetector.getRelativeFilePaths(),
          OPERATION_NAME,
          /* description */ null);

    } else {
      GitUIUtil.notifyError(project,
          getString("action.GitMachete.PullBackgroundable.notification.fail"),
          result.getErrorOutputAsJoinedString(),
          /* important */ true,
          /* error */ null);
      repository.update();
    }
  }

  private static void showUpdates(Project project, VirtualFile root, GitRevisionNumber currentRev, Label beforeLabel) {
    try {
      UpdatedFiles files = UpdatedFiles.create();
      MergeChangeCollector collector = new MergeChangeCollector(project, root, currentRev);
      collector.collect(files);

      GuiUtils.invokeLaterIfNeeded(() -> {
        var manager = ProjectLevelVcsManagerEx.getInstanceEx(project);
        UpdateInfoTree tree = manager.showUpdateProjectInfo(files, OPERATION_NAME, ActionInfo.UPDATE, /* canceled */ false);
        if (tree != null) {
          tree.setBefore(beforeLabel);
          tree.setAfter(LocalHistory.getInstance().putSystemLabel(project, /* name */ "After update"));
          ViewUpdateInfoNotification.focusUpdateInfoTree(project, tree);
        }
      }, ModalityState.defaultModalityState());
    } catch (VcsException e) {
      GitVcs.getInstance(project).showErrors(java.util.Collections.singletonList(e), OPERATION_NAME);
    }
  }
}
