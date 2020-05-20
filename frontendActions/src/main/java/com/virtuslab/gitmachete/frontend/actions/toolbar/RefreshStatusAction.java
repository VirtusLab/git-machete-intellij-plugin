package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.CustomLog;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGraphTable;

@CustomLog
public class RefreshStatusAction extends DumbAwareAction implements IExpectsKeyGraphTable {

  @Override
  public void actionPerformed(AnActionEvent e) {
    LOG.debug("Performing");
    getGraphTable(e).queueRepositoryUpdateAndModelRefresh();
  }
}
