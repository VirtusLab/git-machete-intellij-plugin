package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.virtuslab.gitmachete.backend.api.OngoingRepositoryOperationType.NO_OPERATION;
import static com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus.FETCH_ALL_UP_TO_DATE_TIMEOUT_AS_STRING;
import static com.virtuslab.gitmachete.frontend.actions.traverse.CheckoutAndExecute.checkoutAndExecuteOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.util.ModalityUiUtil;
import git4idea.GitLocalBranch;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FastForwardMergeBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.ResetCurrentToRemoteBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.FetchUpToDateTimeoutStatus;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DivergedFromRemoteDialog;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GitPushDialog;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod(GitMacheteBundle.class)
@CustomLog
public class TraverseSyncToRemote {

  private final Project project;
  private final GitRepository gitRepository;
  private final BaseEnhancedGraphTable graphTable;
  private final IBranchReference branch;
  private final @UI Runnable traverseNextEntry;

  public TraverseSyncToRemote(GitRepository gitRepository,
      BaseEnhancedGraphTable graphTable,
      IBranchReference branch,
      @UI Runnable traverseNextEntry) {
    this.project = gitRepository.getProject();
    this.gitRepository = gitRepository;
    this.graphTable = graphTable;
    this.branch = branch;
    this.traverseNextEntry = traverseNextEntry;
  }

  @ContinuesInBackground
  public void execute() {
    // we need to re-retrieve the gitMacheteBranch as its syncToRemote status could have changed after TraverseSyncToParent
    val repositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
    if (repositorySnapshot == null) {
      LOG.warn("repositorySnapshot is null");
      return;
    }

    val gitMacheteBranch = repositorySnapshot.getManagedBranchByName(branch.getName());
    if (gitMacheteBranch == null) {
      LOG.warn("gitMacheteBranch is null");
      return;
    }

    val ongoingRepositoryOperationType = repositorySnapshot.getOngoingRepositoryOperation().getOperationType();
    if (ongoingRepositoryOperationType != NO_OPERATION) {
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          /* title */ getString("action.GitMachete.BaseTraverseAction.notification.ongoing-operation.title"),
          /* message */ getString("action.GitMachete.BaseTraverseAction.notification.ongoing-operation.message.HTML")
              .fmt(ongoingRepositoryOperationType.toString()));
      return;
    }

    val syncToRemoteStatus = gitMacheteBranch.getRelationToRemote().getSyncToRemoteStatus();
    val localBranchName = gitMacheteBranch.getName();
    val localBranch = gitRepository.getBranches().findLocalBranch(localBranchName);
    if (localBranch == null) {
      LOG.warn("localBranch is null");
      return;
    }

    switch (syncToRemoteStatus) {
      case NoRemotes :
      case InSyncToRemote :
        // A repository refresh isn't needed here.
        // Each side-effecting action like push/rebase is responsible for refreshing repository on its own,
        // so we can assume that the repository is already up to date once we enter void execute().
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, traverseNextEntry);
        break;

      case Untracked :
        @UI Runnable pushUntracked = () -> {
          if (handleUntracked(gitMacheteBranch, localBranch)) {
            graphTable.queueRepositoryUpdateAndModelRefresh(traverseNextEntry);
          }
        };
        checkoutAndExecuteOnUIThread(gitRepository, graphTable, gitMacheteBranch.getName(), pushUntracked);
        break;

      case AheadOfRemote :
        @UI Runnable pushAheadOfRemote = () -> {
          if (handleAheadOfRemote(gitMacheteBranch, localBranch)) {
            graphTable.queueRepositoryUpdateAndModelRefresh(traverseNextEntry);
          }
        };
        checkoutAndExecuteOnUIThread(gitRepository, graphTable, gitMacheteBranch.getName(), pushAheadOfRemote);
        break;

      case DivergedFromAndNewerThanRemote :
      case DivergedFromAndOlderThanRemote :
        @UI Runnable pushForceDiverged = () -> {
          if (handleDivergedFromRemote(gitMacheteBranch, syncToRemoteStatus, localBranch)) {
            graphTable.queueRepositoryUpdateAndModelRefresh(traverseNextEntry);
          }
        };
        checkoutAndExecuteOnUIThread(gitRepository, graphTable, gitMacheteBranch.getName(), pushForceDiverged);
        break;

      case BehindRemote :
        @UI Runnable pullBehindRemote = () -> {
          if (handleBehindRemote(gitMacheteBranch)) {
            graphTable.queueRepositoryUpdateAndModelRefresh(traverseNextEntry);
          }
        };
        checkoutAndExecuteOnUIThread(gitRepository, graphTable, gitMacheteBranch.getName(), pullBehindRemote);
        break;

      default :
        break;
    }
  }

  @UIEffect
  private boolean handleUntracked(IManagedBranchSnapshot gitManagedBranch, GitLocalBranch localBranch) {
    val pushApprovalDialogBuilder = MessageDialogBuilder.yesNoCancel(
        getString("action.GitMachete.BaseTraverseAction.dialog.push-verification.title"),
        getString("action.GitMachete.BaseTraverseAction.dialog.push-verification.untracked.text.HTML")
            .fmt(gitManagedBranch.getName()))
        .cancelText(getString("action.GitMachete.BaseTraverseAction.dialog.cancel-traverse"));

    switch (pushApprovalDialogBuilder.show(project)) {
      case MessageConstants.YES :
        new GitPushDialog(project, gitRepository, GitPushSource.create(localBranch), /* isForcePushRequired */ false).show();
        return true;

      case MessageConstants.NO :
        return true;

      default :
        return false;
    }
  }

  @UIEffect
  private boolean handleAheadOfRemote(IManagedBranchSnapshot gitMacheteBranch, GitLocalBranch localBranch) {
    val remoteTrackingBranch = gitMacheteBranch.getRemoteTrackingBranch();
    assert remoteTrackingBranch != null : "remoteTrackingBranch is null";
    val pushApprovalDialogBuilder = MessageDialogBuilder.yesNoCancel(
        getString("action.GitMachete.BaseTraverseAction.dialog.push-verification.title"),
        getString("action.GitMachete.BaseTraverseAction.dialog.push-verification.ahead.text.HTML")
            .fmt(gitMacheteBranch.getName(), remoteTrackingBranch.getName()))
        .cancelText(getString("action.GitMachete.BaseTraverseAction.dialog.cancel-traverse"));

    switch (pushApprovalDialogBuilder.show(project)) {
      case MessageConstants.YES :
        new GitPushDialog(project, gitRepository, GitPushSource.create(localBranch), /* isForcePushRequired */ false).show();
        return true;

      case MessageConstants.NO :
        return true;

      default :
        return false;
    }
  }

  @ContinuesInBackground
  @UIEffect
  private boolean handleDivergedFromRemote(IManagedBranchSnapshot gitMacheteBranch,
      SyncToRemoteStatus syncToRemoteStatus,
      GitLocalBranch localBranch) {
    val remoteTrackingBranch = gitMacheteBranch.getRemoteTrackingBranch();
    assert remoteTrackingBranch != null : "remoteTrackingBranch is null";
    val selectedAction = new DivergedFromRemoteDialog(project, remoteTrackingBranch, gitMacheteBranch,
        syncToRemoteStatus).showAndGetThePreferredAction();
    if (selectedAction == null) {
      return false;
    }
    switch (selectedAction) {
      case FORCE_PUSH :
        new GitPushDialog(project, gitRepository, GitPushSource.create(localBranch), /* isForcePushRequired */ true).show();
        return true;

      case RESET_TO_REMOTE :
        new ResetCurrentToRemoteBackgroundable(
            getString("action.GitMachete.BaseResetToRemoteAction.task-title"),
            /* canBeCancelled */ true, gitMacheteBranch.getName(), remoteTrackingBranch.getName(),
            gitRepository) {
          @Override
          public void onSuccess() {
            graphTable.queueRepositoryUpdateAndModelRefresh(traverseNextEntry);
          }
        }.queue();
        // The ongoing traverse is now a responsibility of the freshly-queued backgroundable;
        // NOT a responsibility of the outer method.
        return false;

      case DO_NOT_SYNC :
        return true;

      default :
        return false;
    }
  }

  @ContinuesInBackground
  @UIEffect
  private boolean handleBehindRemote(IManagedBranchSnapshot gitMacheteBranch) {
    val remoteTrackingBranch = gitMacheteBranch.getRemoteTrackingBranch();
    assert remoteTrackingBranch != null : "remoteTrackingBranch is null";
    val pullApprovalDialogBuilder = MessageDialogBuilder.yesNoCancel(
        getString("action.GitMachete.BaseTraverseAction.dialog.pull-verification.title"),
        getString("action.GitMachete.BaseTraverseAction.dialog.pull-verification.text.HTML")
            .fmt(gitMacheteBranch.getName(), remoteTrackingBranch.getName()))
        .cancelText(getString("action.GitMachete.BaseTraverseAction.dialog.cancel-traverse"));

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
        Runnable doInUIThreadWhenReady = () -> graphTable.queueRepositoryUpdateAndModelRefresh(traverseNextEntry);
        new FastForwardMergeBackgroundable(gitRepository, mergeProps, fetchNotificationTextPrefix, doInUIThreadWhenReady)
            .queue();
        // The ongoing traverse is now a responsibility of the freshly-queued backgroundable;
        // NOT a responsibility of the outer method.
        return false;

      case MessageConstants.NO :
        return true;

      default :
        return false;
    }
  }
}
