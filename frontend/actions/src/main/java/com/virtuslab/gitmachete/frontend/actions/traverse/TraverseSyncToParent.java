package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.virtuslab.gitmachete.frontend.actions.traverse.CheckoutAndExecute.checkoutAndExecuteOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.util.ModalityUiUtil;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.RebaseOnParentBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.SlideOutBackgroundable;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod({GitMacheteBundle.class, GitVfsUtils.class})
public class TraverseSyncToParent {

  private final Project project;
  private final GitRepository gitRepository;
  private final BaseEnhancedGraphTable graphTable;
  private final IGitMacheteRepositorySnapshot repositorySnapshot;
  private final INonRootManagedBranchSnapshot gitMacheteBranch;
  private final @UI Runnable traverseNextEntry;

  public TraverseSyncToParent(GitRepository gitRepository,
      BaseEnhancedGraphTable graphTable,
      IGitMacheteRepositorySnapshot repositorySnapshot,
      INonRootManagedBranchSnapshot gitMacheteBranch,
      @UI Runnable traverseNextEntry) {
    this.project = gitRepository.getProject();
    this.gitRepository = gitRepository;
    this.graphTable = graphTable;
    this.repositorySnapshot = repositorySnapshot;
    this.gitMacheteBranch = gitMacheteBranch;
    this.traverseNextEntry = traverseNextEntry;
  }

  public void execute() {
    Runnable syncToRemoteRunnable = new TraverseSyncToRemote(gitRepository, graphTable, gitMacheteBranch,
        traverseNextEntry)::execute;

    val syncToParentStatus = gitMacheteBranch.getSyncToParentStatus();
    switch (syncToParentStatus) {
      case InSync :
        // A repository refresh isn't needed here.
        // Each side-effecting action like push/rebase is responsible for refreshing repository on its own,
        // so we can assume that the repository is already up to date once we enter void execute().
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, syncToRemoteRunnable);
        break;

      case MergedToParent :
        @UI Runnable slideOut = () -> {
          if (handleMergedToParent()) {
            graphTable.queueRepositoryUpdateAndModelRefresh(syncToRemoteRunnable);
          }
        };
        // Note that checking out the branch to be slid out has the unfortunate side effect
        // that we won't suggest deleting the branch after the slide out.
        checkoutAndExecuteOnUIThread(gitRepository, graphTable, gitMacheteBranch.getName(), slideOut);
        break;

      case InSyncButForkPointOff :
      case OutOfSync :
        @UI Runnable rebase = () -> {
          if (handleOutOfSyncOrInSyncButForkPointOff(syncToParentStatus, syncToRemoteRunnable)) {
            graphTable.queueRepositoryUpdateAndModelRefresh(syncToRemoteRunnable);
          }
        };
        checkoutAndExecuteOnUIThread(gitRepository, graphTable, gitMacheteBranch.getName(), rebase);
        break;

      default :
        break;
    }
  }

  @UIEffect
  private boolean handleMergedToParent() {
    val branchLayout = repositorySnapshot.getBranchLayout();
    val currentBranchIfManaged = repositorySnapshot.getCurrentBranchIfManaged();
    val slideOutDialog = MessageDialogBuilder.yesNoCancel(
        getString("action.GitMachete.BaseTraverseAction.dialog.merged-to-parent.title"),
        getString(
            "action.GitMachete.BaseTraverseAction.dialog.merged-to-parent.text.HTML").fmt(
                gitMacheteBranch.getName(),
                gitMacheteBranch.getParent().getName()))
        .cancelText(getString("action.GitMachete.BaseTraverseAction.dialog.cancel-traverse"));

    switch (slideOutDialog.show(project)) {
      case MessageConstants.YES :
        new SlideOutBackgroundable(getString("action.GitMachete.BaseSlideOutAction.task.title"),
            gitMacheteBranch, gitRepository, currentBranchIfManaged, branchLayout, graphTable) {
          @Override
          public void onSuccess() {
            graphTable.queueRepositoryUpdateAndModelRefresh(traverseNextEntry);
          }
        }.queue();
        // The ongoing traverse is now a responsibility of the freshly-queued backgroundable;
        // NOT a responsibility of the outer method.
        return false;

      case MessageConstants.NO :
        return true;

      default :
        return false;
    }
  }

  @UIEffect
  private boolean handleOutOfSyncOrInSyncButForkPointOff(SyncToParentStatus syncToParentStatus,
      @UI Runnable syncToRemoteRunnable) {
    var title = getString("action.GitMachete.BaseTraverseAction.dialog.out-of-sync-to-parent.title");
    var text = getString(
        "action.GitMachete.BaseTraverseAction.dialog.out-of-sync-to-parent.text.HTML");

    if (syncToParentStatus == SyncToParentStatus.InSyncButForkPointOff) {
      title = getString("action.GitMachete.BaseTraverseAction.dialog.fork-point-off.title");
      text = getString("action.GitMachete.BaseTraverseAction.dialog.fork-point-off.text.HTML");
    }

    val rebaseDialog = MessageDialogBuilder.yesNoCancel(title,
        text.fmt(
            gitMacheteBranch.getName(),
            gitMacheteBranch.getParent().getName()))
        .cancelText(getString("action.GitMachete.BaseTraverseAction.dialog.cancel-traverse"));

    switch (rebaseDialog.show(project)) {
      case MessageConstants.YES :
        new RebaseOnParentBackgroundable(
            getString("action.GitMachete.BaseSyncToParentByRebaseAction.task-title"),
            gitRepository, repositorySnapshot, gitMacheteBranch, /* shouldExplicitlyCheckout */ false) {
          @Override
          public void onSuccess() {
            graphTable.queueRepositoryUpdateAndModelRefresh(syncToRemoteRunnable);
          }
        }.queue();
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
