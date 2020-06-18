package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.CustomLog;

import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.ui.impl.root.providerservice.GraphTableProvider;

@CustomLog
public class RefreshStatusAction extends DumbAwareAction implements IExpectsKeyProject {

  @Override
  public void actionPerformed(AnActionEvent e) {
    LOG.debug("Performing");
    var project = getProject(e);
    project.getService(GraphTableProvider.class).getGraphTable().queueRepositoryUpdateAndModelRefresh();
  }
}
