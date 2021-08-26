package com.virtuslab.gitmachete.frontend.ui.impl.gitrepositoryselection;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionComponentProvider;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionComponentProviderFactory;

public class GitMacheteGitRepositoryComboBoxFactory implements IGitRepositorySelectionComponentProviderFactory {
  @Override
  public IGitRepositorySelectionComponentProvider create(Project project) {
    return new GitRepositoryComboBox(project);
  }
}
