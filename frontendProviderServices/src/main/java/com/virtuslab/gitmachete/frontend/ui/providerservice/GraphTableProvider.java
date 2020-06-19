package com.virtuslab.gitmachete.frontend.ui.providerservice;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableFactory;

@Service
public final class GraphTableProvider {

  @Getter
  private final BaseGraphTable graphTable;

  public GraphTableProvider(Project project) {
    this.graphTable = RuntimeBinding
        .instantiateSoleImplementingClass(IGraphTableFactory.class)
        .create(project);
  }
}
