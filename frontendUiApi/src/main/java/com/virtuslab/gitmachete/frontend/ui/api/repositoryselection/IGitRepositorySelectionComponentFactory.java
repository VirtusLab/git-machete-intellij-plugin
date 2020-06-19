package com.virtuslab.gitmachete.frontend.ui.api.repositoryselection;

import com.intellij.openapi.project.Project;

public interface IGitRepositorySelectionComponentFactory {
  ISelectionComponent create(Project project);
}
