package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.MERGE;
import static git4idea.update.GitUpdateSessionKt.getBodyForUpdateNotification;
import static git4idea.update.GitUpdateSessionKt.getTitleForUpdateNotification;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import git4idea.GitBranch;
import git4idea.branch.GitBranchPair;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector;
import git4idea.commands.GitUntrackedFilesOverwrittenByOperationDetector;
import git4idea.repo.GitRepository;
import git4idea.update.GitUpdateInfoAsLog;
import git4idea.update.GitUpdatedRanges;
import git4idea.util.GitUntrackedFilesHelper;
import git4idea.util.LocalChangesWouldBeOverwrittenHelper;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.val;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public abstract class GitCommandUpdatingCurrentBranchBackgroundable extends SideEffectingBackgroundable {

  // Plugin-owned notification display-id passed to LocalChangesWouldBeOverwrittenHelper.
  // The id only feeds notification settings/telemetry bookkeeping (Settings -> Notifications grouping
  // and FUS) - it's not part of the notification's title, body, severity or actions, so picking a
  // plugin-namespaced value keeps the user-facing behavior identical while leaving the plugin fully
  // decoupled from git4idea's internal id naming.
  public static final String LOCAL_CHANGES_DETECTED_DISPLAY_ID = "git-machete.local-changes-detected";

  protected final GitRepository gitRepository;

  public GitCommandUpdatingCurrentBranchBackgroundable(
      GitRepository gitRepository,
      @Untainted String taskTitle,
      @Untainted String shortName) {
    super(gitRepository.getProject(), taskTitle, shortName);
    this.gitRepository = gitRepository;
  }

  protected abstract LambdaLogger log();

  protected abstract @Untainted @I18nFormat({}) String getOperationName();

  protected abstract String getTargetBranchName();

  @UIThreadUnsafe
  protected abstract @Nullable GitLineHandler createGitLineHandler();

  @Override
  @UIThreadUnsafe
  public final void doRun(ProgressIndicator indicator) {
    val handler = createGitLineHandler();
    if (handler == null) {
      return;
    }
    val localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(
        gitRepository.getRoot(), MERGE);
    val untrackedFilesDetector = new GitUntrackedFilesOverwrittenByOperationDetector(
        gitRepository.getRoot());
    handler.addLineListener(localChangesDetector);
    handler.addLineListener(untrackedFilesDetector);

    GitUpdatedRanges updatedRanges = deriveGitUpdatedRanges(getTargetBranchName());

    try (AccessToken ignore = DvcsUtil.workingTreeChangeStarted(project, getOperationName())) {
      GitCommandResult result = Git.getInstance().runCommand(handler);
      handleResult(result, localChangesDetector, untrackedFilesDetector, updatedRanges);
    }
  }

  @UIThreadUnsafe
  private @Nullable GitUpdatedRanges deriveGitUpdatedRanges(String targetBranchName) {
    GitUpdatedRanges updatedRanges = null;
    val currentBranch = gitRepository.getCurrentBranch();
    if (currentBranch != null) {
      GitBranch targetBranch = gitRepository.getBranches().findBranchByName(targetBranchName);
      if (targetBranch != null) {
        GitBranchPair refPair = new GitBranchPair(currentBranch, targetBranch);
        updatedRanges = GitUpdatedRanges.calcInitialPositions(project,
            java.util.Collections.singletonMap(gitRepository, refPair));
      } else {
        log().warn("Couldn't find the branch with name '${targetBranchName}'");
      }
    }
    return updatedRanges;
  }

  @UIThreadUnsafe
  private void handleResult(
      GitCommandResult result,
      GitLocalChangesWouldBeOverwrittenDetector localChangesDetector,
      GitUntrackedFilesOverwrittenByOperationDetector untrackedFilesDetector,
      @Nullable GitUpdatedRanges updatedRanges) {
    val root = gitRepository.getRoot();
    if (result.success()) {
      VfsUtil.markDirtyAndRefresh(/* async */ false, /* recursive */ true, /* reloadChildren */ false, root);
      gitRepository.update();
      // updatedRanges is null only when the ref pair couldn't be assembled (detached HEAD, or the
      // target branch is unknown to git4idea). In that edge case we skip the post-update notification
      // entirely - the VFS refresh + repo update above are still enough to keep the UI consistent.
      if (updatedRanges != null) {
        val ranges = updatedRanges.calcCurrentPositions();
        GitUpdateInfoAsLog.NotificationData notificationData = new GitUpdateInfoAsLog(project, ranges)
            .calculateDataAndCreateLogTab();

        Notification notification;
        if (notificationData != null) {
          val title = getTitleForUpdateNotification(notificationData.getUpdatedFilesCount(),
              notificationData.getReceivedCommitsCount());
          val content = getBodyForUpdateNotification(notificationData.getFilteredCommitsCount());
          notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(title,
              content,
              INFORMATION);
          notification.addAction(NotificationAction.createSimple(getString(
              "action.GitMachete.GitCommandUpdatingCurrentBranchBackgroundable.notification.message.view-commits"),
              notificationData.getViewCommitAction()));

        } else {
          // When the pull results with no commits, there is no git update info (as log).
          // Based on that we know that all files are up-to-date.
          notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
              getString(
                  "action.GitMachete.GitCommandUpdatingCurrentBranchBackgroundable.notification.title.all-files-are-up-to-date"),
              /* content */ "", INFORMATION);
        }
        VcsNotifier.getInstance(project).notify(notification);
      }

    } else if (localChangesDetector.wasMessageDetected()) {
      LocalChangesWouldBeOverwrittenHelper.showErrorNotification(project,
          LOCAL_CHANGES_DETECTED_DISPLAY_ID,
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
      VcsNotifier.getInstance(project).notifyError(
          /* displayId */ null,
          GitMacheteBundle.fmt(
              getString("action.GitMachete.GitCommandUpdatingCurrentBranchBackgroundable.notification.title.update-fail"),
              getOperationName()),
          result.getErrorOutputAsJoinedString());
      gitRepository.update();
    }
  }
}
