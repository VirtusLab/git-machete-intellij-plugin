package com.virtuslab.gitmachete.frontend.ui.providerservice;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IBaseEnhancedGraphTableFactory;

@Service
public final class GraphTableProvider {

  @Getter
  private final BaseEnhancedGraphTable graphTable;

  public GraphTableProvider(Project project) {
    this.graphTable = RuntimeBinding
        .instantiateSoleImplementingClass(IBaseEnhancedGraphTableFactory.class)
        .create(project);
  }
}
