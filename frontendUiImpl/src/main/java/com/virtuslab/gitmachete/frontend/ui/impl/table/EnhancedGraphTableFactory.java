package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IBaseEnhancedGraphTableFactory;

public class EnhancedGraphTableFactory implements IBaseEnhancedGraphTableFactory {
  @Override
  public BaseEnhancedGraphTable create(Project project) {
    return new EnhancedGraphTable(project);
  }
}
