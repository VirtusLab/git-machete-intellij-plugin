package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.InSyncToRemote;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BasePullAction;

public class PullCurrentAction extends BasePullAction {
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

    val isBehindOrInSyncToRemote = getCurrentBranchNameIfManaged(anActionEvent)
        .flatMap(bn -> getManagedBranchByName(anActionEvent, bn))
        .map(b -> b.getRelationToRemote().getSyncToRemoteStatus())
        .map(strs -> List.of(BehindRemote, InSyncToRemote).contains(strs))
        .getOrElse(false);

    presentation.setVisible(isBehindOrInSyncToRemote);
  }
}
