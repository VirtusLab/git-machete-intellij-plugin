package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.frontend.actions.common.ActionUtils.getGraphTable;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.EnhancedLambdaLoggerFactory;
import com.virtuslab.logger.IEnhancedLambdaLogger;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GRAPH_TABLE}</li>
 * </ul>
 */
public class RefreshStatusAction extends DumbAwareAction {
  private static final IEnhancedLambdaLogger LOG = EnhancedLambdaLoggerFactory.create();

  @Override
  public void actionPerformed(AnActionEvent e) {
    LOG.debug("Performing");
    getGraphTable(e).queueRepositoryUpdateAndModelRefresh();
  }
}
