package com.virtuslab.gitmachete.frontend.ui.providerservice;

import javax.swing.JComponent;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.ui.api.repositoryselection.IGitRepositorySelectionComponentFactory;
import com.virtuslab.gitmachete.frontend.ui.api.repositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.api.repositoryselection.ISelectionComponent;

@Service
public final class SelectedVcsRepositoryProvider {

  private final ISelectionComponent selectionComponent;

  public SelectedVcsRepositoryProvider(Project project) {
    this.selectionComponent = RuntimeBinding
        .instantiateSoleImplementingClass(IGitRepositorySelectionComponentFactory.class)
        .create(project);
  }

  public Option<GitRepository> getSelectedVcsRepository() {
    return selectionComponent.getSelectedRepository();
  }

  public JComponent getSelectionComponent() {
    return selectionComponent.getComponent();
  }

  public IGitRepositorySelectionProvider getGitRepositorySelectionProvider() {
    return selectionComponent;
  }
}
