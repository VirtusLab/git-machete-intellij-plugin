package com.virtuslab.gitmachete.frontend.actions.base;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;

public interface ISyncToRemoteStatusDependentAction extends IBranchNameProvider, IExpectsKeyGitMacheteRepository {
  String getActionName();

  default String getDescriptionActionName() {
    return getActionName();
  }

  default String getEnabledDescriptionBundleKey() {
    return "action.description.enabled";
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
      presentation.setDescription(
          GitMacheteBundle.message("action.description.disabled.undefined.branch.name", getDescriptionActionName()));
      return;
    }

    var gitMacheteBranchByName = getGitMacheteBranchByName(anActionEvent, branchName.get());
    if (gitMacheteBranchByName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          GitMacheteBundle.message("action.description.disabled.undefined.machete.branch", getDescriptionActionName()));
      return;
    }
    var syncToRemoteStatus = gitMacheteBranchByName.map(branch -> branch.getSyncToRemoteStatus());

    if (syncToRemoteStatus.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          GitMacheteBundle.message("action.description.disabled.undefined.sync-to-remote", getDescriptionActionName()));
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
    var isRelationEligible = getEligibleRelations().contains(relation);

    if (isRelationEligible) {
      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(GitMacheteBundle.message("action.text.current-branch", getActionName()));
      }

      var enabledDesc = GitMacheteBundle.message(getEnabledDescriptionBundleKey(), getDescriptionActionName(),
          branchName.get());
      presentation.setDescription(enabledDesc);

    } else {
      presentation.setEnabled(false);

      var desc = Match(relation).of(
          Case($(SyncToRemoteStatus.Relation.AheadOfRemote),
              GitMacheteBundle.message("synctoremotestatus.relation.aheadofremote")),
          Case($(SyncToRemoteStatus.Relation.BehindRemote),
              GitMacheteBundle.message("synctoremotestatus.relation.behindremote")),
          Case($(SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote),
              GitMacheteBundle.message("synctoremotestatus.relation.divergedfrom.andnewerthanremote")),
          Case($(SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote),
              GitMacheteBundle.message("synctoremotestatus.relation.divergedfrom.andolderthanremote")),
          Case($(SyncToRemoteStatus.Relation.InSyncToRemote),
              GitMacheteBundle.message("synctoremotestatus.relation.insynctoremote")),
          Case($(SyncToRemoteStatus.Relation.Untracked),
              GitMacheteBundle.message("synctoremotestatus.relation.untracked")),
          Case($(), GitMacheteBundle.message("synctoremotestatus.relation.unknown", relation.toString())));
      presentation.setDescription(
          GitMacheteBundle.message("action.description.disabled.branch.status", getDescriptionActionName(), desc));
    }
  }
}
