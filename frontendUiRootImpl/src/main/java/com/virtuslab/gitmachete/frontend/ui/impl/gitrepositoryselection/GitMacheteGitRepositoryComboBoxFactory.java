package com.virtuslab.gitmachete.frontend.ui.impl.gitrepositoryselection;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionComponent;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionComponentFactory;

public class GitMacheteGitRepositoryComboBoxFactory implements IGitRepositorySelectionComponentFactory {
  @Override
  public IGitRepositorySelectionComponent create(Project project) {
    return new GitRepositoryComboBox(project);
  }
}
