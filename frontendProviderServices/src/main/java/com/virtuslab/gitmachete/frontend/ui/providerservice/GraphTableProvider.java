package com.virtuslab.gitmachete.frontend.ui.providerservice;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.ui.api.table.AbstractEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IAbstractEnhancedGraphTableFactory;

@Service
public final class GraphTableProvider {

  @Getter
  private final AbstractEnhancedGraphTable graphTable;

  public GraphTableProvider(Project project) {
    this.graphTable = RuntimeBinding
        .instantiateSoleImplementingClass(IAbstractEnhancedGraphTableFactory.class)
        .create(project);
  }
}
