package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getBranchLayout;
import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGraphTable;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_GRAPH_TABLE}</li>
 * </ul>
 */
@CustomLog
public class ToggleListingCommitsAction extends ToggleAction implements DumbAware {

  @Override
  @UIEffect
  public void update(AnActionEvent e) {
    super.update(e);

    var branchLayout = getBranchLayout(e);
    if (branchLayout.isDefined()) {
      boolean anyChildBranchExists = branchLayout.get().getRootEntries()
          .exists(rootBranch -> rootBranch.getSubentries().nonEmpty());
      var presentation = e.getPresentation();
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
  public boolean isSelected(AnActionEvent e) {
    return getGraphTable(e).isListingCommits();
  }

  @Override
  @UIEffect
  public void setSelected(AnActionEvent e, boolean state) {
    LOG.debug("Triggered with state = ${state}");
    var graphTable = getGraphTable(e);
    graphTable.setListingCommits(state);
    graphTable.refreshModel();
  }
}
