package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IBaseGraphTableFactory;

public class EnhancedGraphTableFactory implements IBaseGraphTableFactory {
  @Override
  public BaseGraphTable create(Project project) {
    return new EnhancedGraphTable(project);
  }
}
