package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.InSyncToRemote;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.collection.List;
import lombok.val;
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

    val presentation = anActionEvent.getPresentation();
    if (!presentation.isVisible()) {
      return;
    }

    val managedBranchByName = getManagedBranchByName(anActionEvent, getCurrentBranchNameIfManaged(anActionEvent));
    val syncToRemoteStatus = managedBranchByName != null
        ? managedBranchByName.getRelationToRemote().getSyncToRemoteStatus()
        : null;

    val isBehindOrInSyncToRemote = syncToRemoteStatus != null
        && List.of(BehindRemote, InSyncToRemote).contains(syncToRemoteStatus);

    presentation.setVisible(isBehindOrInSyncToRemote);
  }
}
