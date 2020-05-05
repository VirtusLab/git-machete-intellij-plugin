package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableFactory;

public class GitMacheteGraphTableFactory implements IGraphTableFactory {
  @Override
  public BaseGraphTable create(Project project, IGitRepositorySelectionProvider gitRepositorySelectionProvider) {
    return new GitMacheteGraphTable(project, gitRepositorySelectionProvider);
  }
}
