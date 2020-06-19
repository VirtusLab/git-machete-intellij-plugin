package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.CustomLog;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;

@CustomLog
public class RefreshStatusAction extends DumbAwareAction implements IExpectsKeyProject {

  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    LOG.debug("Performing");
    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }
}
