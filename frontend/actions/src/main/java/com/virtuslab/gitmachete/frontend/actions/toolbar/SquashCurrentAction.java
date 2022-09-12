package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.InSyncButForkPointOff;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BaseSquashAction;

public class SquashCurrentAction extends BaseSquashAction {
  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    final var presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    final var branchName = getNameOfBranchUnderAction(anActionEvent);
    final var managedBranch = getManagedBranchByName(anActionEvent, branchName);
    final var nonRootBranch = managedBranch != null && managedBranch.isNonRoot()
        ? managedBranch.asNonRoot()
        : null;
    final var syncToParentStatus = nonRootBranch != null ? nonRootBranch.getSyncToParentStatus() : null;

    if (branchName != null && nonRootBranch == null) {
      presentation.setVisible(false);

    } else if (branchName != null && nonRootBranch != null) { //nonRootBranch is certainly not null.
      final var numberOfCommits = nonRootBranch.getCommits().length();

      if (numberOfCommits < 2 || syncToParentStatus == InSyncButForkPointOff) {
        presentation.setVisible(false);
      } else {
        presentation.setText(getString("action.GitMachete.BaseSquashAction.text"));
        presentation.setDescription(getNonHtmlString("action.GitMachete.SquashCurrentAction.description"));
      }
    }
  }
}
