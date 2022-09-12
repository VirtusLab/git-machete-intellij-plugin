package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndOlderThanRemote;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BaseResetToRemoteAction;

public class ResetCurrentToRemoteAction extends BaseResetToRemoteAction {
  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    final var presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    final var managedBranchByName = getManagedBranchByName(anActionEvent, getCurrentBranchNameIfManaged(anActionEvent));

    final var isDivergedFromAndOlderThanRemote = managedBranchByName != null
        && managedBranchByName.getRelationToRemote().getSyncToRemoteStatus() == DivergedFromAndOlderThanRemote;

    presentation.setVisible(isDivergedFromAndOlderThanRemote);
  }
}
