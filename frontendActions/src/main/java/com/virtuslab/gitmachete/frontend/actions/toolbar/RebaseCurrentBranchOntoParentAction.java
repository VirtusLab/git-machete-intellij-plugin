package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
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
    var presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    var currentBranch = getCurrentBranchNameIfManaged(anActionEvent)
        .flatMap(bn -> getManagedBranchByName(anActionEvent, bn));

    var isNonRootBranch = currentBranch.map(b -> b.isNonRoot()).getOrElse(false);

    @SuppressWarnings("confirmedbranchitem") var isNotMerged = currentBranch
        .filter(branch -> branch.isNonRoot())
        .map(branch -> branch.asNonRoot().getSyncToParentStatus() != SyncToParentStatus.MergedToParent)
        .getOrElse(false);

    presentation.setVisible(isNonRootBranch && isNotMerged);
  }
}
