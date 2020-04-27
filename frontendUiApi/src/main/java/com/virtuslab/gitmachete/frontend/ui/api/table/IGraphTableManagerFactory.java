package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.selection.IGitRepositorySelectionProvider;

public interface IGraphTableManagerFactory {
  IGraphTableManager create(Project project, IGitRepositorySelectionProvider gitRepositorySelectionProvider);
}
