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

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;

public interface ISyncToParentStatusDependentAction extends IBranchNameProviderWithoutLogging, IExpectsKeyGitMacheteRepository {

  @I18nFormat({})
  String getActionNameForDisabledDescription();

  /**
   * @return a format string for description of action in enabled state
   *         where {@code {1}} corresponds to branch name as returned by {@link #getNameOfBranchUnderActionWithoutLogging}
   *         and {@code {0}} corresponds to name of its parent branch
   */
  @I18nFormat({GENERAL, GENERAL})
  String getEnabledDescriptionFormat();

  /**
   * This method provides a list of {@link SyncToParentStatus}s for which the action should be ENABLED.
   * Note that this "enability" matches actions in any place (both toolbar and context menu in particular).
   * Visibility itself may be switched off (even though the action is enabled).
   *
   * As we want to avoid an overpopulation of the toolbar we make some actions there INVISIBLE
   * but ENABLED (since they still shall be available from {@code Find Action...}).
   *
   * @return a list of statuses for which the action should be enabled (not necessarily visible)
   */
  List<SyncToParentStatus> getEligibleStatuses();

  @UIEffect
  default void syncToParentStatusDependentActionUpdate(AnActionEvent anActionEvent) {

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabled()) {
      return;
    }

    var branchName = getNameOfBranchUnderActionWithoutLogging(anActionEvent).getOrNull();
    var gitMacheteBranchByName = branchName != null
        ? getGitMacheteBranchByNameWithoutLogging(anActionEvent, branchName).getOrNull()
        : null;

    if (branchName == null || gitMacheteBranchByName == null) {
      presentation.setEnabled(false);
      presentation.setDescription(
          format(getString("action.GitMachete.description.disabled.undefined.machete-branch"),
              getActionNameForDisabledDescription(), getQuotedStringOrCurrent(branchName)));
      return;
    } else if (gitMacheteBranchByName.isRoot()) {
      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
        presentation.setEnabled(false);
        presentation.setDescription(
            format(getString("action.GitMachete.description.disabled.branch-is-root"), getActionNameForDisabledDescription()));
      } else { // contextmenu
        // in case of root branch we do not want to show this option at all
        presentation.setEnabledAndVisible(false);
      }
      return;
    }

    var gitMacheteNonRootBranch = gitMacheteBranchByName.asNonRoot();
    var syncToParentStatus = gitMacheteNonRootBranch.getSyncToParentStatus();

    var isStatusEligible = getEligibleStatuses().contains(syncToParentStatus);

    if (isStatusEligible) {
      var parentName = gitMacheteNonRootBranch.getParent().getName();
      var enabledDesc = format(getEnabledDescriptionFormat(), parentName, branchName);
      presentation.setDescription(enabledDesc);

    } else {
      presentation.setEnabled(false);

      // @formatter:off
      var desc = Match(syncToParentStatus).of(
          Case($(SyncToParentStatus.InSync),
              getString("action.GitMachete.ISyncToParentStatusDependentAction.description.sync-to-parent-status.in-sync")),
          Case($(SyncToParentStatus.InSyncButForkPointOff),
              getString("action.GitMachete.ISyncToParentStatusDependentAction.description.sync-to-parent-status.in-sync-but-fork-point-off")),
          Case($(SyncToParentStatus.MergedToParent),
              getString("action.GitMachete.ISyncToParentStatusDependentAction.description.sync-to-parent-status.merged-to-parent")),
          Case($(SyncToParentStatus.OutOfSync),
              getString("action.GitMachete.ISyncToParentStatusDependentAction.description.sync-to-parent-status.out-of-sync")),
          Case($(),
              format(getString("action.GitMachete.ISyncToParentStatusDependentAction.description.sync-to-parent-status.unknown"), syncToParentStatus.toString())));
      // @formatter:on

      presentation.setDescription(
          format(getString("action.GitMachete.ISyncToParentStatusDependentAction.description.disabled.branch-status"),
              getActionNameForDisabledDescription(), desc));
    }
  }
}
