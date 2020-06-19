package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;

@CustomLog
public class ToggleListingCommitsAction extends ToggleAction
    implements
      DumbAware,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeyProject {

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);

    var branchLayout = getBranchLayout(anActionEvent);
    if (branchLayout.isDefined()) {
      boolean anyChildBranchExists = branchLayout.get().getRootEntries()
          .exists(rootBranch -> rootBranch.getSubentries().nonEmpty());
      var presentation = anActionEvent.getPresentation();
      if (anyChildBranchExists) {
        presentation.setEnabled(true);
        presentation.setDescription("Toggle listing commits");
      } else {
        presentation.setEnabled(false);
        presentation.setDescription("Toggle listing commits disabled: no child branches present");
      }
    }
  }

  @Override
  @UIEffect
  public boolean isSelected(AnActionEvent anActionEvent) {
    return getGraphTable(anActionEvent).isListingCommits();
  }

  @Override
  @UIEffect
  public void setSelected(AnActionEvent anActionEvent, boolean state) {
    LOG.debug("Triggered with state = ${state}");

    var graphTable = getGraphTable(anActionEvent);
    graphTable.setListingCommits(state);
    graphTable.refreshModel();
  }
}
