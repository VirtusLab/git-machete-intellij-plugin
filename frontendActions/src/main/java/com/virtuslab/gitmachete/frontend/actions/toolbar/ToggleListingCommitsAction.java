package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.DumbAware;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseGitMacheteRepositoryReadyAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class ToggleListingCommitsAction extends BaseGitMacheteRepositoryReadyAction
    implements
      DumbAware,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeyProject {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    boolean selected = isSelected(anActionEvent);
    var presentation = anActionEvent.getPresentation();
    Toggleable.setSelected(presentation, selected);
    if (!presentation.isEnabledAndVisible()) {
      return;
    }

    var branchLayout = getBranchLayout(anActionEvent);
    if (branchLayout.isDefined()) {
      boolean anyChildBranchExists = branchLayout.get().getRootEntries()
          .exists(rootBranch -> rootBranch.getChildren().nonEmpty());
      if (anyChildBranchExists) {
        presentation.setEnabled(true);
        presentation.setDescription("Toggle listing commits");
      } else {
        presentation.setEnabled(false);
        presentation.setDescription("Toggle listing commits disabled: no child branches present");
      }
    } else {
      presentation.setEnabled(false);
      presentation.setDescription("Toggle listing commits disabled: no branches present");
    }
  }

  @Override
  @UIEffect
  public final void actionPerformed(AnActionEvent e) {
    boolean state = !isSelected(e);
    setSelected(e, state);
    var presentation = e.getPresentation();
    Toggleable.setSelected(presentation, state);
  }

  @UIEffect
  public boolean isSelected(AnActionEvent anActionEvent) {
    return getGraphTable(anActionEvent).isListingCommits();
  }

  @UIEffect
  public void setSelected(AnActionEvent anActionEvent, boolean state) {
    log().debug("Triggered with state = ${state}");

    var graphTable = getGraphTable(anActionEvent);
    graphTable.setListingCommits(state);
    graphTable.refreshModel();
  }
}
