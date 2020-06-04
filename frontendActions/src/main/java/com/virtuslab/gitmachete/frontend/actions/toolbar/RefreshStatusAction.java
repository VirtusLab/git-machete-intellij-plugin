package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGraphTable;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public class RefreshStatusAction extends DumbAwareAction implements IExpectsKeyGraphTable {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  @Override
  public void actionPerformed(AnActionEvent e) {
    LOG.debug("Performing");
    getGraphTable(e).queueRepositoryUpdateAndModelRefresh();
  }
}
