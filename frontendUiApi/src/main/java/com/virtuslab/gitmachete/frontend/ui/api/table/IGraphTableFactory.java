package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionProvider;

public interface IGraphTableFactory {
  BaseGraphTable create(Project project, IGitRepositorySelectionProvider gitRepositorySelectionProvider);
}
