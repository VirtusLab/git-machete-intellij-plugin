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

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;

public interface ISyncToParentStatusDependentAction extends IBranchNameProvider, IExpectsKeyGitMacheteRepository {
  @I18nFormat({})
  String getActionName();

  @I18nFormat({})
  String getDescriptionActionName();

  @I18nFormat({GENERAL, GENERAL})
  String getEnabledDescriptionFormat();

  String getCurrentBranchText();

  List<SyncToParentStatus> getEligibleStatuses();

  @UIEffect
  default void syncToParentStatusDependentActionUpdate(AnActionEvent anActionEvent) {

    Presentation presentation = anActionEvent.getPresentation();
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchName = getNameOfBranchUnderAction(anActionEvent);
    if (branchName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          format(getString("action.GitMachete.description.disabled.undefined.branch-name"), getDescriptionActionName()));
      return;
    }

    var gitMacheteBranchByName = getGitMacheteBranchByName(anActionEvent, branchName.get());
    if (gitMacheteBranchByName.isEmpty()) {
      presentation.setEnabled(false);
      presentation.setDescription(
          format(getString("action.GitMachete.description.disabled.undefined.machete-branch"), getDescriptionActionName()));
      return;
    } else if (gitMacheteBranchByName.get().isRootBranch()) {

      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_TOOLBAR)) {
        presentation.setEnabled(false);
        presentation.setDescription(
            format(getString("action.GitMachete.description.disabled.branch-is-root"), getDescriptionActionName()));
      } else { // contextmenu
        // in case of root branch we do not want to show this option at all
        presentation.setEnabledAndVisible(false);
      }
      return;
    }

    var gitMacheteNonRootBranch = gitMacheteBranchByName.get().asNonRootBranch();
    var syncToParentStatus = gitMacheteNonRootBranch.getSyncToParentStatus();

    var isStatusEligible = getEligibleStatuses().contains(syncToParentStatus);

    if (isStatusEligible) {
      if (getCurrentBranchNameIfManaged(anActionEvent).equals(branchName)) {
        presentation.setText(getCurrentBranchText());
      }

      var parentName = gitMacheteNonRootBranch.getParentBranch().getName();
      var enabledDesc = format(getEnabledDescriptionFormat(), parentName, branchName.get());
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
      presentation.setDescription(
          format(getString("action.GitMachete.ISyncToParentStatusDependentAction.description.disabled.branch-status"), getDescriptionActionName(), desc));
      // @formatter:on
    }
  }
}
