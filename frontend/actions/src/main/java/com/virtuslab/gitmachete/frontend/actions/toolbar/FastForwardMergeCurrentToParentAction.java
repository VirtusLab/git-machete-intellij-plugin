package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseFastForwardMergeToParentAction;

public class FastForwardMergeCurrentToParentAction extends BaseFastForwardMergeToParentAction {
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

    final var currentBranchByName = getManagedBranchByName(anActionEvent, getCurrentBranchNameIfManaged(anActionEvent));
    final var nonRootBranch = currentBranchByName != null && currentBranchByName.isNonRoot()
        ? currentBranchByName.asNonRoot()
        : null;

    final var isInSyncToParent = nonRootBranch != null && nonRootBranch.getSyncToParentStatus() == SyncToParentStatus.InSync;

    presentation.setVisible(isInSyncToParent);
  }
}
