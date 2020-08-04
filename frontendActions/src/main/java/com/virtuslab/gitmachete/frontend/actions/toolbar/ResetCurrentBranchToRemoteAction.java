package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.*;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseResetBranchToRemoteAction;

public class ResetCurrentBranchToRemoteAction extends BaseResetBranchToRemoteAction {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    var isEnabledAndVisible = getNameOfBranchUnderAction(anActionEvent)
        .flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn))
        .map(b -> b.getSyncToRemoteStatus().getRelation() == DivergedFromAndOlderThanRemote)
        .getOrElse(false);

    anActionEvent.getPresentation().setEnabledAndVisible(isEnabledAndVisible);

    if (isEnabledAndVisible) {
      super.onUpdate(anActionEvent);
    }
  }
}
