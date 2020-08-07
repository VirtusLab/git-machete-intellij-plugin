package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseSlideOutBranchAction;

public class SlideOutCurrentBranchAction extends BaseSlideOutBranchAction {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    var isMergedToParent = getNameOfBranchUnderAction(anActionEvent)
        .flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn))
        .flatMap(b -> b.isNonRootBranch() ? Option.some(b.asNonRootBranch()) : Option.none())
        .map(nrb -> nrb.getSyncToParentStatus() == SyncToParentStatus.MergedToParent)
        .getOrElse(false);

    anActionEvent.getPresentation().setEnabledAndVisible(isMergedToParent);

    if (isMergedToParent) {
      super.onUpdate(anActionEvent);
    }
  }
}
