package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
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

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    IManagedBranchSnapshot currentBranchByName = getManagedBranchByName(anActionEvent,
        getCurrentBranchNameIfManaged(anActionEvent));
    val nonRootBranch = currentBranchByName != null && currentBranchByName.isNonRoot() ? currentBranchByName.asNonRoot() : null;

    val isInSyncToParent = nonRootBranch != null && nonRootBranch.getSyncToParentStatus() == SyncToParentStatus.InSync;

    presentation.setVisible(isInSyncToParent);
  }
}
