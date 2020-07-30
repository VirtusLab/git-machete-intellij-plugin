package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.text.MessageFormat.format;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;

public interface ISyncToRemoteStatusDependentAction extends IBranchNameProvider, IExpectsKeyGitMacheteRepository {
  @I18nFormat({})
  String getActionName();

  @I18nFormat({})
  default String getActionNameForDescription() {
    return getActionName();
  }

  @I18nFormat({GENERAL, GENERAL})
  default String getEnabledDescriptionFormat() {
    return getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.enabled");
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
          format(getString("action.GitMachete.description.disabled.undefined.branch-name"), getActionNameForDescription()));
      return;
    }

    var gitMacheteBranchByName = getGitMacheteBranchByName(anActionEvent, branchName.get());
    if (gitMacheteBranchByName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          format(getString("action.GitMachete.description.disabled.undefined.machete-branch"), getActionNameForDescription()));
      return;
    }
    var syncToRemoteStatus = gitMacheteBranchByName.map(branch -> branch.getSyncToRemoteStatus());

    if (syncToRemoteStatus.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(format(
          getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.disabled.undefined.sync-to-remote"),
          getActionNameForDescription()));
      return;
    }

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.get().getRelation();
    var isRelationEligible = getEligibleRelations().contains(relation);

    if (isRelationEligible) {
      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(
            format(getString("action.GitMachete.ISyncToRemoteStatusDependentAction.text.current-branch"), getActionName()));
      }

      var enabledDesc = format(getEnabledDescriptionFormat(), getActionNameForDescription(), branchName.get());
      presentation.setDescription(enabledDesc);

    } else {
      presentation.setEnabled(false);

      // @formatter:off
      var desc = Match(relation).of(
          Case($(SyncToRemoteStatus.Relation.AheadOfRemote),
              getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.sync-to-remote-status.relation.ahead-of-remote")),
          Case($(SyncToRemoteStatus.Relation.BehindRemote),
              getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.sync-to-remote-status.relation.behind-remote")),
          Case($(SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote),
              getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.sync-to-remote-status.relation.diverged-from.and-newer-than-remote")),
          Case($(SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote),
              getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.sync-to-remote-status.relation.diverged-from.and-older-than-remote")),
          Case($(SyncToRemoteStatus.Relation.InSyncToRemote),
              getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.sync-to-remote-status.relation.in-sync-to-remote")),
          Case($(SyncToRemoteStatus.Relation.Untracked),
              getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.sync-to-remote-status.relation.untracked")),
          Case($(),
              format(getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.sync-to-remote-status.relation.unknown"), relation.toString())));

      presentation.setDescription(
          format(getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.disabled.branch-status"), getActionNameForDescription(), desc));
      // @formatter:on
    }
  }
}
