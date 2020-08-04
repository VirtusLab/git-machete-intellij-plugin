package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.base.BasePullBranchFastForwardOnlyAction;

public class PullCurrentBranchFastForwardOnlyAction extends BasePullBranchFastForwardOnlyAction {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    var isEnabledAndVisible = getNameOfBranchUnderAction(anActionEvent)
        .flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn))
        .map(b -> b.getSyncToRemoteStatus().getRelation() == SyncToRemoteStatus.Relation.BehindRemote)
        .getOrElse(false);

    anActionEvent.getPresentation().setEnabledAndVisible(isEnabledAndVisible);

    if (isEnabledAndVisible) {
      super.onUpdate(anActionEvent);
    }
  }
}
