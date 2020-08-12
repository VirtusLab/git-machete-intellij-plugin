package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getQuotedStringOrCurrent;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
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

  /**
   * This method provides a list of {@link SyncToRemoteStatus.Relation}s for which the action should be ENABLED.
   * Note that this "enability" matches actions in any place (both toolbar and context menu in particular).
   * Visibility itself may be switched off (even though the action is enabled).
   *
   * As we want to avoid an overpopulation of the toolbar we make some actions there INVISIBLE
   * but ENABLED (since they still shall be available from {@code Find Action...}).
   *
   * @return a list of relations for which the action should be enabled (not necessarily visible)
   */
  List<SyncToRemoteStatus.Relation> getEligibleRelations();

  @UIEffect
  default void syncToRemoteStatusDependentActionUpdate(AnActionEvent anActionEvent) {

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabled()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    var gitMacheteBranch = branchName != null
        ? getGitMacheteBranchByName(anActionEvent, branchName).getOrNull()
        : null;

    if (branchName == null || gitMacheteBranch == null) {
      presentation.setEnabled(false);
      presentation.setDescription(
          format(getString("action.GitMachete.description.disabled.undefined.machete-branch"),
              getActionNameForDescription(), getQuotedStringOrCurrent(branchName)));
      return;
    }
    var syncToRemoteStatus = gitMacheteBranch.getSyncToRemoteStatus();

    SyncToRemoteStatus.Relation relation = syncToRemoteStatus.getRelation();
    var isRelationEligible = getEligibleRelations().contains(relation);

    if (isRelationEligible) {
      // At this point `branchName` must be present, so `.getOrNull()` is here only to satisfy checker framework
      var enabledDesc = format(getEnabledDescriptionFormat(), getActionNameForDescription(), branchName);
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
      // @formatter:on

      presentation.setDescription(
          format(getString("action.GitMachete.ISyncToRemoteStatusDependentAction.description.disabled.branch-status"),
              getActionNameForDescription(), desc));
    }
  }
}
