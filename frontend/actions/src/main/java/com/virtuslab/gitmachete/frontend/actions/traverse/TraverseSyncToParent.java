package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.MessageDialogBuilder;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
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
  private final Runnable traverseNextEntry;

  public TraverseSyncToParent(GitRepository gitRepository,
      BaseEnhancedGraphTable graphTable,
      IGitMacheteRepositorySnapshot repositorySnapshot,
      INonRootManagedBranchSnapshot gitMacheteBranch,
      Runnable traverseNextEntry) {
    this.project = gitRepository.getProject();
    this.gitRepository = gitRepository;
    this.graphTable = graphTable;
    this.repositorySnapshot = repositorySnapshot;
    this.gitMacheteBranch = gitMacheteBranch;
    this.traverseNextEntry = traverseNextEntry;
  }

  @UIEffect
  public void execute() {
    @UI Runnable syncToRemoteRunnable = new TraverseSyncToRemote(gitRepository, graphTable, gitMacheteBranch,
        traverseNextEntry)::execute;

    switch (gitMacheteBranch.getSyncToParentStatus()) {
      case MergedToParent :
        if (!handleMergedToParent()) {
          return;
        }
        break;

      case InSyncButForkPointOff :
      case OutOfSync :
        if (!handleOutOfSyncOrInSyncButForkPointOff(syncToRemoteRunnable)) {
          return;
        }
        break;

      default :
        break;
    }

    graphTable.queueRepositoryUpdateAndModelRefresh(syncToRemoteRunnable);
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
        // the ongoing traverse is now down to the newly-created backgroundable; NOT down to outer method
        return false;

      case MessageConstants.NO :
        return true;

      default :
        return false;
    }
  }

  @UIEffect
  private boolean handleOutOfSyncOrInSyncButForkPointOff(@UI Runnable syncToRemoteRunnable) {
    val rebaseDialog = MessageDialogBuilder.yesNoCancel(
        getString("action.GitMachete.BaseTraverseAction.dialog.diverged-from-parent.title"),
        getString(
            "action.GitMachete.BaseTraverseAction.dialog.diverged-from-parent.text.HTML").fmt(
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
        // the ongoing traverse is now down to the newly-created backgroundable; NOT down to outer method
        return false;

      case MessageConstants.NO :
        return true;

      default :
        return false;
    }
  }

}
