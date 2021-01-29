package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
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
import git4idea.util.GitUntrackedFilesHelper;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public abstract class GitCommandUpdatingCurrentBranchBackgroundable extends Task.Backgroundable {

  protected final Project project;
  protected final GitRepository gitRepository;

  public GitCommandUpdatingCurrentBranchBackgroundable(
      Project project,
      GitRepository gitRepository,
      String taskTitle) {
    super(project, taskTitle, /* canBeCancelled */ true);
    this.project = project;
    this.gitRepository = gitRepository;
  }

  protected abstract @I18nFormat({}) String getOperationName();

  protected abstract String getTargetBranchName();

  @UIThreadUnsafe
  protected abstract @Nullable GitLineHandler createGitLineHandler();

  @Override
  @UIThreadUnsafe
  public final void run(ProgressIndicator indicator) {
    var handler = createGitLineHandler();
    if (handler == null) {
      return;
    }
    var localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(gitRepository.getRoot(), MERGE);
    var untrackedFilesDetector = new GitUntrackedFilesOverwrittenByOperationDetector(gitRepository.getRoot());
    handler.addLineListener(localChangesDetector);
    handler.addLineListener(untrackedFilesDetector);

    Label beforeLabel = LocalHistory.getInstance().putSystemLabel(project, /* name */ "Before update");

    GitUpdatedRanges updatedRanges = deriveGitUpdatedRanges(getTargetBranchName());

    String beforeRevision = gitRepository.getCurrentRevision();
    try (AccessToken ignore = DvcsUtil.workingTreeChangeStarted(project, getOperationName())) {
      GitCommandResult result = Git.getInstance().runCommand(handler);

      if (beforeRevision != null) {
        GitRevisionNumber currentRev = new GitRevisionNumber(beforeRevision);
        handleResult(result,
            localChangesDetector,
            untrackedFilesDetector,
            currentRev,
            beforeLabel,
            updatedRanges);
      }
    }
  }

  @UIThreadUnsafe
  private @Nullable GitUpdatedRanges deriveGitUpdatedRanges(String remoteBranchName) {
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

  @UIThreadUnsafe
  private void handleResult(
      GitCommandResult result,
      GitLocalChangesWouldBeOverwrittenDetector localChangesDetector,
      GitUntrackedFilesOverwrittenByOperationDetector untrackedFilesDetector,
      GitRevisionNumber currentRev,
      Label beforeLabel,
      @Nullable GitUpdatedRanges updatedRanges) {
    VirtualFile root = gitRepository.getRoot();
    if (result.success()) {
      VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ true, /* reloadChildren */ false, root);
      gitRepository.update();
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
              "action.GitMachete.GitCommandUpdatingCurrentBranchBackgroundable.notification.message.view-commits"),
              notificationData.getViewCommitAction()));

        } else {
          // When the pull results with no commits, there is no git update info (as log).
          // Based on that we know that all files are up-to-date.
          notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
              getString(
                  "action.GitMachete.GitCommandUpdatingCurrentBranchBackgroundable.notification.title.all-files-are-up-to-date"),
              /* content */ "", INFORMATION, /* listener */ null);
        }
        VcsNotifier.getInstance(project).notify(notification);

      } else {
        showUpdates(currentRev, beforeLabel);
      }

    } else if (localChangesDetector.wasMessageDetected()) {
      LocalChangesWouldBeOverwrittenHelper.showErrorNotification(project,
          gitRepository.getRoot(),
          getOperationName(),
          localChangesDetector.getRelativeFilePaths());

    } else if (untrackedFilesDetector.wasMessageDetected()) {
      GitUntrackedFilesHelper.notifyUntrackedFilesOverwrittenBy(project,
          root,
          untrackedFilesDetector.getRelativeFilePaths(),
          getOperationName(),
          /* description */ null);

    } else {
      var notifier = VcsNotifier.getInstance(project);
      notifier.notifyError(
          format(getString("action.GitMachete.GitCommandUpdatingCurrentBranchBackgroundable.notification.title.update-fail"),
              getOperationName()),
          result.getErrorOutputAsJoinedString());
      gitRepository.update();
    }
  }

  // TODO (#496): replace with a non-reflective constructor call
  @SneakyThrows
  @UIThreadUnsafe
  private static MergeChangeCollector createMergeChangeCollector(
      Project project, GitRepository repository, GitRevisionNumber start) {
    try {
      // Proper solution for 2020.2+
      var constructor = MergeChangeCollector.class.getConstructor(Project.class, GitRepository.class, GitRevisionNumber.class);
      return constructor.newInstance(project, repository, start);
    } catch (NoSuchMethodException e) {
      // Fallback for 2020.1 (also available on 2020.2, but scheduled for removal in 2020.3)
      var constructor = MergeChangeCollector.class.getConstructor(Project.class, VirtualFile.class, GitRevisionNumber.class);
      return constructor.newInstance(project, repository.getRoot(), start);
    }
  }

  @UIThreadUnsafe
  private void showUpdates(GitRevisionNumber currentRev, Label beforeLabel) {
    try {
      UpdatedFiles files = UpdatedFiles.create();

      var collector = createMergeChangeCollector(project, gitRepository, currentRev);
      collector.collect(files);

      GuiUtils.invokeLaterIfNeeded(() -> {
        var manager = ProjectLevelVcsManagerEx.getInstanceEx(project);
        UpdateInfoTree tree = manager.showUpdateProjectInfo(files, getOperationName(), ActionInfo.UPDATE, /* canceled */ false);
        if (tree != null) {
          tree.setBefore(beforeLabel);
          tree.setAfter(LocalHistory.getInstance().putSystemLabel(project, /* name */ "After update"));
          ViewUpdateInfoNotification.focusUpdateInfoTree(project, tree);
        }
      }, ModalityState.defaultModalityState());
    } catch (VcsException e) {
      GitVcs.getInstance(project).showErrors(java.util.Collections.singletonList(e), getOperationName());
    }
  }
}
