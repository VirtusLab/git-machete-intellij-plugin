package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import com.virtuslab.binding.RuntimeBinding;

@Service
@Getter
public final class GraphTableProvider {

  private final BaseGraphTable graphTable;

  private final VcsRootComboBox vcsRootComboBox;

  public GraphTableProvider(Project project) {
    vcsRootComboBox = new VcsRootComboBox(project);
    this.graphTable = RuntimeBinding
        .instantiateSoleImplementingClass(IGraphTableFactory.class)
        .create(project, vcsRootComboBox);
  }
}
