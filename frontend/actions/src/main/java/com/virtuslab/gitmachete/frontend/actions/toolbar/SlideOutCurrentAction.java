package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseSlideOutAction;

public class SlideOutCurrentAction extends BaseSlideOutAction {
  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    val presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    val managedBranchByName = getManagedBranchByName(anActionEvent, getCurrentBranchNameIfManaged(anActionEvent));
    val nonRootBranch = managedBranchByName != null && managedBranchByName.isNonRoot()
        ? managedBranchByName.asNonRoot()
        : null;
    val isMergedToParent = nonRootBranch != null
        && nonRootBranch.getSyncToParentStatus() == SyncToParentStatus.MergedToParent;

    presentation.setVisible(isMergedToParent);
  }
}
