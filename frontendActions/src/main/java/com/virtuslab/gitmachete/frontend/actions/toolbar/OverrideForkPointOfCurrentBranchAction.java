package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BaseOverrideForkPointAction;

public class OverrideForkPointOfCurrentBranchAction extends BaseOverrideForkPointAction {
  @Override
  public Option<String> getNameOfBranchUnderActionWithLogging(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManagedWithLogging(anActionEvent);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    var presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    var isInSyncButForkPointOff = getCurrentBranchNameIfManaged(anActionEvent)
        .flatMap(bn -> getGitMacheteBranchByNameWithLogging(anActionEvent, bn))
        .flatMap(b -> b.isNonRoot() ? Option.some(b.asNonRoot()) : Option.none())
        .map(nrb -> nrb.getSyncToParentStatus() == SyncToParentStatus.InSyncButForkPointOff)
        .getOrElse(false);

    presentation.setVisible(isInSyncButForkPointOff);
  }
}
