package com.virtuslab.gitmachete.frontend.ui.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IBaseEnhancedGraphTableFactory;

@Service
public final class GraphTableService {

  @Getter
  private final BaseEnhancedGraphTable graphTable;

  public GraphTableService(Project project) {
    this.graphTable = ApplicationManager.getApplication().getService(IBaseEnhancedGraphTableFactory.class)
        .create(project);
  }
}
