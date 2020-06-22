package com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection;

import com.intellij.openapi.project.Project;

public interface IGitRepositorySelectionComponentProviderFactory {
  IGitRepositorySelectionComponentProvider create(Project project);
}
