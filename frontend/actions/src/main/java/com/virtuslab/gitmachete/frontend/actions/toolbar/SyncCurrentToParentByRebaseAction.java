package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseSyncToParentByRebaseAction;

public class SyncCurrentToParentByRebaseAction extends BaseSyncToParentByRebaseAction {
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

    final var currentBranch = getManagedBranchByName(anActionEvent, getCurrentBranchNameIfManaged(anActionEvent));

    if (currentBranch != null) {
      if (currentBranch.isNonRoot()) {
        final var isNotMerged = currentBranch.asNonRoot().getSyncToParentStatus() != SyncToParentStatus.MergedToParent;
        presentation.setVisible(isNotMerged);
      } else {
        presentation.setVisible(false);
      }
    } else {
      presentation.setVisible(false);
    }
  }
}
