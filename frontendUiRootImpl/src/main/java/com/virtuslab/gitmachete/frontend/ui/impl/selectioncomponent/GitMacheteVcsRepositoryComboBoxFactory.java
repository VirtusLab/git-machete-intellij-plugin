package com.virtuslab.gitmachete.frontend.ui.impl.selectioncomponent;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.repositoryselection.IGitRepositorySelectionComponentFactory;
import com.virtuslab.gitmachete.frontend.ui.api.repositoryselection.ISelectionComponent;

public class GitMacheteVcsRepositoryComboBoxFactory implements IGitRepositorySelectionComponentFactory {
  @Override
  public ISelectionComponent create(Project project) {
    return new VcsRepositoryComboBox(project);
  }
}
