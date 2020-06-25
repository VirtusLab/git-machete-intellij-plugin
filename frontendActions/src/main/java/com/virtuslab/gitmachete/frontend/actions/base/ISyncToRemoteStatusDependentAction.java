package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.SyncToRemoteStatusDescriptionProvider.syncToRemoteStatusRelationToReadableBranchDescription;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;

public interface ISyncToRemoteStatusDependentAction extends IBranchNameProvider, IExpectsKeyGitMacheteRepository {
  String getActionName();

  default String getDescriptionActionName() {
    return getActionName();
  }

  List<SyncToRemoteStatus.Relation> getEligibleRelations();

  @UIEffect
  default void syncToRemoteStatusDependentActionUpdate(AnActionEvent anActionEvent) {

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent);

    if (branchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getDescriptionActionName() + "disabled due to undefined branch");
      return;
    }

    var syncToRemoteStatus = getGitMacheteBranchByName(anActionEvent, branchName.get())
        .map(branch -> branch.getSyncToRemoteStatus());

    if (syncToRemoteStatus.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(getDescriptionActionName() + "disabled due to undefined sync to remote status");
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
    var isRelationEligible = getEligibleRelations().contains(relation);

    if (isRelationEligible) {

      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(getActionName() + " Current Branch");
      }

      presentation
          .setDescription(getDescriptionActionName() + " branch '" + branchName.get() + "' to its remote tracking branch");

    } else {
      presentation.setEnabled(false);
      String descriptionSpec = syncToRemoteStatusRelationToReadableBranchDescription(relation);
      presentation.setDescription(getDescriptionActionName() + " disabled because " + descriptionSpec);
    }
  }
}
