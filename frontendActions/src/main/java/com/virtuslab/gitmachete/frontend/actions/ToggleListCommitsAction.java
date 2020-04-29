package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.actions.ActionUtils.getBranchLayout;
import static com.virtuslab.gitmachete.frontend.actions.ActionUtils.getGraphTableManager;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_BRANCH_LAYOUT}</li>
 *  <li>{@link DataKeys#KEY_GRAPH_TABLE_MANAGER}</li>
 * </ul>
 */
public class ToggleListCommitsAction extends ToggleAction implements DumbAware {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  @UIEffect
  public void update(AnActionEvent e) {
    super.update(e);

    var branchLayout = getBranchLayout(e);
    if (branchLayout.isDefined()) {
      boolean anyChildBranchExists = branchLayout.get().getRootEntries()
          .exists(rootBranch -> rootBranch.getSubentries().nonEmpty());
      e.getPresentation().setEnabled(anyChildBranchExists);
    }
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    return getGraphTableManager(e).isListingCommits();
  }

  @Override
  @UIEffect
  public void setSelected(AnActionEvent e, boolean state) {
    LOG.debug("Triggered with state = ${state}");
    var graphTableManager = getGraphTableManager(e);
    graphTableManager.setListingCommits(state);
    graphTableManager.refreshGraphTable();
  }
}
