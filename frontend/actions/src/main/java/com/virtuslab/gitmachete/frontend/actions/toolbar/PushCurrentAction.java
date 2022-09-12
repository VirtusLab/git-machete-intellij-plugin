package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Untracked;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BasePushAction;

public class PushCurrentAction extends BasePushAction {
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
        ? managedBranchByName
            .getRelationToRemote().getSyncToRemoteStatus()
        : null;

    final var isAheadOrDivergedAndNewerOrUntracked = syncToRemoteStatus != null
        && List.of(AheadOfRemote, DivergedFromAndNewerThanRemote, Untracked).contains(syncToRemoteStatus);

    presentation.setVisible(isAheadOrDivergedAndNewerOrUntracked);
  }
}
