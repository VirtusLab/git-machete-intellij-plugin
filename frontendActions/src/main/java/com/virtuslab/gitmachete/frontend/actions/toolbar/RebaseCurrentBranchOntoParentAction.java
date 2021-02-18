package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseRebaseBranchOntoParentAction;

public class RebaseCurrentBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
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

    val currentBranch = getCurrentBranchNameIfManaged(anActionEvent)
        .flatMap(bn -> getManagedBranchByName(anActionEvent, bn)).getOrNull();

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
}
