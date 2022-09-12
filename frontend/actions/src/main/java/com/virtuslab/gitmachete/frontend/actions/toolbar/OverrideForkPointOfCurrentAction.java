package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseOverrideForkPointAction;

public class OverrideForkPointOfCurrentAction extends BaseOverrideForkPointAction {
  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    final var managedBranchByName = getManagedBranchByName(anActionEvent, getCurrentBranchNameIfManaged(anActionEvent));
    final var nonRootBranch = managedBranchByName != null && managedBranchByName.isNonRoot()
        ? managedBranchByName.asNonRoot()
        : null;

    final var isInSyncButForkPointOff = nonRootBranch != null
        && nonRootBranch.getSyncToParentStatus() == SyncToParentStatus.InSyncButForkPointOff;

    presentation.setVisible(isInSyncButForkPointOff);
  }
}
