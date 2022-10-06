package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus.FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.MessageDialogBuilder;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.ResetCurrentToRemoteBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.FastForwardMerge;
import com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DivergedFromRemoteDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GitPushDialog;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;

@ExtensionMethod(GitMacheteBundle.class)
public final class TraverseSyncToRemote {

  private TraverseSyncToRemote() {}

  @UIEffect
  static void syncBranchToRemote(Project project, BaseEnhancedGraphTable graphTable,
      @Nullable IRemoteTrackingBranchReference remoteTrackingBranch, String branchName, GitRepository gitRepository,
      @UI Runnable traverseNextEntry) {
    val repositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
    val gitMacheteBranch = repositorySnapshot != null ? repositorySnapshot.getManagedBranchByName(branchName) : null;
    if (gitMacheteBranch == null || remoteTrackingBranch == null) {
      // t0d0: remoteTrackingBranch cen be null for Untracked
      graphTable.queueRepositoryUpdateAndModelRefresh(() -> traverseNextEntry.run());
      return;
    }
    val syncToRemoteStatus = gitMacheteBranch.getRelationToRemote().getSyncToRemoteStatus();
    val localBranchName = gitMacheteBranch.getName();
    val localBranch = gitRepository.getBranches().findLocalBranch(localBranchName);
    if (localBranch == null) {
      return;
    }
    val remoteTrackingBranchName = remoteTrackingBranch.getName();
    switch (syncToRemoteStatus) {
      case AheadOfRemote :
      case Untracked :
        val pushApprovalDialogBuilder = MessageDialogBuilder.yesNoCancel(
            getString("action.GitMachete.TraverseAction.dialog.push-verification.title"),
            getString("action.GitMachete.TraverseAction.dialog.push-verification.text.HTML")
                .fmt(localBranchName, remoteTrackingBranchName))
            .cancelText(getString("action.GitMachete.TraverseAction.dialog.cancel-traverse"));

        switch (pushApprovalDialogBuilder.show(project)) {
          case MessageConstants.YES :
            new GitPushDialog(project, List.of(gitRepository), GitPushSource.create(localBranch),
                /* isForcePushRequired */ false)
                    .show();
            break;

          case MessageConstants.NO :
            break;

          default :
            break;
        }
        break;

      case DivergedFromAndOlderThanRemote :
      case DivergedFromAndNewerThanRemote :
        val selectedAction = new DivergedFromRemoteDialog(project, remoteTrackingBranch, gitMacheteBranch,
            syncToRemoteStatus).showAndGetThePreferredAction();
        if (selectedAction == null) {
          return;
        }
        switch (selectedAction) {
          case FORCE_PUSH :
            new GitPushDialog(project, List.of(gitRepository), GitPushSource.create(localBranch),
                /* isForcePushRequired */ true).show();
            break;

          case RESET_ON_REMOTE :
            new ResetCurrentToRemoteBackgroundable(project,
                getString("action.GitMachete.BaseResetToRemoteAction.task-title"),
                /* canBeCancelled */ true, localBranchName, remoteTrackingBranchName, gitRepository);
            break;

          default :
            break;
        }
        break;

      case BehindRemote :
        val pullApprovalDialogBuilder = MessageDialogBuilder.yesNoCancel(
            getString("action.GitMachete.TraverseAction.dialog.pull-verification.title"),
            getString("action.GitMachete.TraverseAction.dialog.pull-verification.text.HTML")
                .fmt(gitMacheteBranch.getName(), remoteTrackingBranch.getName()));

        switch (pullApprovalDialogBuilder.show(project)) {
          case MessageConstants.YES :
            val mergeProps = new MergeProps(
                /* movingBranchName */ gitMacheteBranch,
                /* stayingBranchName */ remoteTrackingBranch);

            val isUpToDate = FetchUpToDateTimeoutStatus.isUpToDate(gitRepository);
            val fetchNotificationPrefix = isUpToDate
                ? getNonHtmlString("action.GitMachete.BasePullAction.notification.prefix.no-fetch-perform")
                    .fmt(FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING)
                : getNonHtmlString("action.GitMachete.BasePullAction.notification.prefix.fetch-perform");
            val fetchNotificationTextPrefix = fetchNotificationPrefix + (fetchNotificationPrefix.isEmpty() ? "" : " ");
            FastForwardMerge.createBackgroundable(project, gitRepository, mergeProps, fetchNotificationTextPrefix).queue();
            break;

          case MessageConstants.NO :
            break;

          default :
            break;
        }
        break;

      default :
        break;
    }
    graphTable.queueRepositoryUpdateAndModelRefresh(() -> traverseNextEntry.run());
  }
}
