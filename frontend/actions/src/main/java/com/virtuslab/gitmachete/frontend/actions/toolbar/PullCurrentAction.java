package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.InSyncToRemote;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BasePullAction;

public class PullCurrentAction extends BasePullAction {
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
    final var syncToRemoteStatus = managedBranchByName != null
        ? managedBranchByName.getRelationToRemote().getSyncToRemoteStatus()
        : null;

    final var isBehindOrInSyncToRemote = syncToRemoteStatus != null
        && List.of(BehindRemote, InSyncToRemote).contains(syncToRemoteStatus);

    presentation.setVisible(isBehindOrInSyncToRemote);
  }
}
