package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseResetBranchToRemoteAction;

public class ResetCurrentBranchToRemoteAction extends BaseResetBranchToRemoteAction {
  @Override
  public Option<String> getNameOfBranchUnderActionWithoutLogging(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManagedWithoutLogging(anActionEvent);
  }

  @Override
  public Option<String> getNameOfBranchUnderActionWithLogging(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManagedWithLogging(anActionEvent);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    var presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    var isDivergedFromAndOlderThanRemote = getCurrentBranchNameIfManagedWithoutLogging(anActionEvent)
        .flatMap(bn -> getGitMacheteBranchByNameWithoutLogging(anActionEvent, bn))
        .map(b -> b.getSyncToRemoteStatus().getRelation() == DivergedFromAndOlderThanRemote)
        .getOrElse(false);

    presentation.setVisible(isDivergedFromAndOlderThanRemote);
  }
}
