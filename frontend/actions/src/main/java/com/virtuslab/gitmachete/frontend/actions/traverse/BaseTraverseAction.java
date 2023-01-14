package com.virtuslab.gitmachete.frontend.actions.traverse;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.base.IBranchNameProvider;
import com.virtuslab.gitmachete.frontend.actions.dialogs.DoNotAskOption;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;

@ExtensionMethod({GitVfsUtils.class, GitMacheteBundle.class})
public abstract class BaseTraverseAction extends BaseGitMacheteRepositoryReadyAction implements IBranchNameProvider {

  public static final String SHOW_TRAVERSE_INFO = "git-machete.traverse.approval.show";

  @Override
  protected boolean isSideEffecting() {
    return true;
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    val presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    presentation.setDescription(getNonHtmlString("action.GitMachete.BaseTraverseAction.description"));

    val graphTable = getGraphTable(anActionEvent);
    val repositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
    val branchLayout = repositorySnapshot != null ? repositorySnapshot.getBranchLayout() : null;

    if (repositorySnapshot == null || branchLayout == null || branchLayout.getRootEntries().isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getNonHtmlString("action.GitMachete.BaseTraverseAction.description.empty-layout"));
      return;
    }

    String branchUnderAction = getNameOfBranchUnderAction(anActionEvent);
    if (branchUnderAction == null) {
      presentation.setEnabled(false);
      presentation
          .setDescription(getNonHtmlString("action.GitMachete.BaseTraverseAction.description.current-branch-unmanaged"));
      return;
    }

    boolean anythingToBeDone = false;
    for (var branch = branchUnderAction; branch != null; branch = branchLayout.findNextEntryName(branch)) {
      val managedBranch = repositorySnapshot.getManagedBranchByName(branch);
      if (managedBranch == null) {
        continue;
      }
      if (managedBranch.isNonRoot() && managedBranch.asNonRoot().getSyncToParentStatus() != SyncToParentStatus.InSync) {
        anythingToBeDone = true;
        break;
      }
      val syncToRemoteStatus = managedBranch.getRelationToRemote().getSyncToRemoteStatus();
      if (syncToRemoteStatus != SyncToRemoteStatus.InSyncToRemote && syncToRemoteStatus != SyncToRemoteStatus.NoRemotes) {
        anythingToBeDone = true;
        break;
      }
    }
    if (!anythingToBeDone) {
      presentation.setEnabled(false);
      presentation.setDescription(
          getNonHtmlString("action.GitMachete.BaseTraverseAction.description.nothing-to-be-done").fmt(branchUnderAction));
    }
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val graphTable = getGraphTable(anActionEvent);
    val repositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
    val branchLayout = repositorySnapshot != null ? repositorySnapshot.getBranchLayout() : null;
    val project = getProject(anActionEvent);

    boolean yesNoResult = true;
    if (branchLayout != null && branchLayout.getRootEntries().nonEmpty() && gitRepository != null) {
      if (PropertiesComponent.getInstance(project).getBoolean(SHOW_TRAVERSE_INFO, /* defaultValue */ true)) {

        val traverseInfoDialog = MessageDialogBuilder.okCancel(
            getString("action.GitMachete.BaseTraverseAction.dialog.traverse-approval.title"),
            getString("action.GitMachete.BaseTraverseAction.dialog.traverse-approval.text.HTML"))
            .icon(Messages.getInformationIcon())
            .doNotAsk(new DoNotAskOption(project, SHOW_TRAVERSE_INFO));

        yesNoResult = traverseInfoDialog.ask(project);
      }

      if (yesNoResult) {
        val initialBranchName = getNameOfBranchUnderAction(anActionEvent);
        if (initialBranchName != null) {
          traverseFrom(gitRepository, graphTable, initialBranchName);
        } else {
          log().warn("Skipping traverse action because initialBranchName is undefined");
        }
      }
    }
  }

  @ContinuesInBackground
  private void traverseFrom(GitRepository gitRepository, BaseEnhancedGraphTable graphTable, String branchName) {
    val repositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
    if (repositorySnapshot == null) {
      return;
    }
    val branchLayout = repositorySnapshot.getBranchLayout();
    val gitMacheteBranch = repositorySnapshot.getManagedBranchByName(branchName);

    if (gitMacheteBranch != null) {
      Runnable traverseNextEntry = () -> {
        var nextBranch = branchLayout != null ? branchLayout.findNextEntry(branchName) : null;
        if (nextBranch != null) {
          traverseFrom(gitRepository, graphTable, nextBranch.getName());
        }
      };

      if (gitMacheteBranch.isNonRoot()) {
        new TraverseSyncToParent(gitRepository, graphTable, gitMacheteBranch, traverseNextEntry)
            .execute();
      } else {
        new TraverseSyncToRemote(gitRepository, graphTable, gitMacheteBranch, traverseNextEntry).execute();
      }
    }
  }
}
