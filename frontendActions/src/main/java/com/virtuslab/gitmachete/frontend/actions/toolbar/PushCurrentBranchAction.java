package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BasePushBranchAction;

public class PushCurrentBranchAction extends BasePushBranchAction {
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

    val isAheadOrDivergedAndNewerOrUntracked = getCurrentBranchNameIfManaged(anActionEvent)
        .flatMap(bn -> getManagedBranchByName(anActionEvent, bn))
        .map(b -> b.getSyncToRemoteStatus().getRelation())
        .map(strs -> List.of(AheadOfRemote, DivergedFromAndNewerThanRemote, Untracked).contains(strs))
        .getOrElse(false);

    presentation.setVisible(isAheadOrDivergedAndNewerOrUntracked);
  }
}
