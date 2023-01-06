package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseSyncToParentByRebaseAction;

@CustomLog
public class SyncCurrentToParentByRebaseAction extends BaseSyncToParentByRebaseAction {
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

    val currentBranch = getManagedBranchByName(anActionEvent, getCurrentBranchNameIfManaged(anActionEvent));

    if (currentBranch != null) {
      if (currentBranch.isNonRoot()) {
        val isNotMerged = currentBranch.asNonRoot().getSyncToParentStatus() != SyncToParentStatus.MergedToParent;
        presentation.setVisible(isNotMerged);
      } else {
        presentation.setVisible(false);
      }
    } else {
      presentation.setVisible(false);
    }
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }
}
