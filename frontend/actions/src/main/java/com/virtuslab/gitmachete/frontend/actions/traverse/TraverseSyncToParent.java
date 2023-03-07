package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.frontend.actions.traverse.CheckoutAndExecute.checkoutAndExecuteOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.util.ModalityUiUtil;
import git4idea.repo.GitRepository;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.RebaseOnParentBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.common.SlideOut;
import com.virtuslab.gitmachete.frontend.actions.dialogs.TraverseStepConfirmationDialog;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;

@CustomLog
@ExtensionMethod({GitMacheteBundle.class, GitVfsUtils.class})
@SuppressWarnings("MissingSwitchDefault")
public class TraverseSyncToParent {

  private final Project project;
  private final GitRepository gitRepository;
  private final BaseEnhancedGraphTable graphTable;
  private final IBranchReference branch;
  private final @UI Runnable traverseNextEntry;

  public TraverseSyncToParent(GitRepository gitRepository,
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
    if (gitMacheteBranch.isRoot()) {
      LOG.warn("gitMacheteBranch is root");
      return;
    }

    Runnable syncToRemoteRunnable = new TraverseSyncToRemote(gitRepository, graphTable, branch, traverseNextEntry)::execute;

    val syncToParentStatus = gitMacheteBranch.asNonRoot().getSyncToParentStatus();
    val syncToRemoteStatus = gitMacheteBranch.getRelationToRemote().getSyncToRemoteStatus();
    switch (syncToParentStatus) {
      case InSync :
        // A repository refresh isn't needed here.
        // Each side-effecting action like push/rebase is responsible for refreshing repository on its own,
        // so we can assume that the repository is already up to date once we enter void execute().
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, syncToRemoteRunnable);
        break;

      case MergedToParent :
        @UI Runnable slideOut = () -> handleMergedToParent(repositorySnapshot, gitMacheteBranch.asNonRoot(),
            syncToRemoteRunnable);
        // Note that checking out the branch to be slid out has the unfortunate side effect
        // that we won't suggest deleting the branch after the slide out.
        checkoutAndExecuteOnUIThread(gitRepository, graphTable, branch.getName(), slideOut);
        break;

      case InSyncButForkPointOff :
      case OutOfSync :
        if (syncToRemoteStatus == DivergedFromAndOlderThanRemote) {
          ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, syncToRemoteRunnable);
        } else {
          @UI Runnable rebase = () -> handleOutOfSyncOrInSyncButForkPointOff(repositorySnapshot, gitMacheteBranch.asNonRoot(),
              syncToRemoteRunnable);
          checkoutAndExecuteOnUIThread(gitRepository, graphTable, branch.getName(), rebase);
        }
        break;

      default :
        break;
    }
  }

  @ContinuesInBackground
  @UIEffect
  private void handleMergedToParent(
      IGitMacheteRepositorySnapshot repositorySnapshot,
      INonRootManagedBranchSnapshot managedBranch,
      Runnable syncToRemoteRunnable) {
    val branchLayout = repositorySnapshot.getBranchLayout();
    val title = getString("action.GitMachete.BaseTraverseAction.dialog.merged-to-parent.title");
    val message = getString(
        "action.GitMachete.BaseTraverseAction.dialog.merged-to-parent.text.HTML").fmt(
            managedBranch.getName(),
            managedBranch.getParent().getName());
    val slideOutDialog = new TraverseStepConfirmationDialog(title, message);

    switch (slideOutDialog.show(project)) {
      case YES :
        // For a branch merged to its parent, we're not syncing to remote.
        // Let's just go straight to the next branch.
        Runnable doInUIThreadWhenReady = () -> graphTable.queueRepositoryUpdateAndModelRefresh(traverseNextEntry);
        new SlideOut(managedBranch, gitRepository, branchLayout, graphTable).run(doInUIThreadWhenReady);
        break;

      case NO :
        graphTable.queueRepositoryUpdateAndModelRefresh(syncToRemoteRunnable);
        break;
    }
  }

  @ContinuesInBackground
  @UIEffect
  private void handleOutOfSyncOrInSyncButForkPointOff(
      IGitMacheteRepositorySnapshot repositorySnapshot,
      INonRootManagedBranchSnapshot managedBranch,
      Runnable syncToRemoteRunnable) {
    var title = getString("action.GitMachete.BaseTraverseAction.dialog.out-of-sync-to-parent.title");
    var text = getString("action.GitMachete.BaseTraverseAction.dialog.out-of-sync-to-parent.text.HTML");

    if (managedBranch.getSyncToParentStatus() == SyncToParentStatus.InSyncButForkPointOff) {
      title = getString("action.GitMachete.BaseTraverseAction.dialog.fork-point-off.title");
      text = getString("action.GitMachete.BaseTraverseAction.dialog.fork-point-off.text.HTML");
    }

    val message = text.fmt(managedBranch.getName(), managedBranch.getParent().getName());
    val rebaseDialog = new TraverseStepConfirmationDialog(title, message);

    switch (rebaseDialog.show(project)) {
      case YES :
        new RebaseOnParentBackgroundable(
            gitRepository, repositorySnapshot, managedBranch, /* shouldExplicitlyCheckout */ false) {
          @Override
          @ContinuesInBackground
          public void onSuccess() {
            graphTable.queueRepositoryUpdateAndModelRefresh(syncToRemoteRunnable);
          }
        }.queue();
        break;

      case NO :
        graphTable.queueRepositoryUpdateAndModelRefresh(syncToRemoteRunnable);
        break;
    }
  }

}
