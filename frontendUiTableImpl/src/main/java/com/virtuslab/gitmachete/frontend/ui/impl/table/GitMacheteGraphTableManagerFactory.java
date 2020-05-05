package com.virtuslab.gitmachete.frontend.ui.impl.table;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManager;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManagerFactory;

public class GitMacheteGraphTableManagerFactory implements IGraphTableManagerFactory {
  @Override
  public IGraphTableManager create(BaseGraphTable graphTable, Project project,
      IGitRepositorySelectionProvider gitRepositorySelectionProvider) {
    return new GitMacheteGraphTableManager(graphTable, project, gitRepositorySelectionProvider);
  }
}
