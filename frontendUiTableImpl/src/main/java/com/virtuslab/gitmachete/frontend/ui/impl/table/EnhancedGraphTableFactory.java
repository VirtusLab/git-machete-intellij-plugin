package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.table.AbstractEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IAbstractEnhancedGraphTableFactory;

public class EnhancedGraphTableFactory implements IAbstractEnhancedGraphTableFactory {
  @Override
  public AbstractEnhancedGraphTable create(Project project) {
    return new EnhancedGraphTable(project);
  }
}
