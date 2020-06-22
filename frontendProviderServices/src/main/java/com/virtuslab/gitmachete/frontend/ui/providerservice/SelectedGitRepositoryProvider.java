package com.virtuslab.gitmachete.frontend.ui.providerservice;

import javax.swing.JComponent;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionChangeObserver;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionComponent;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionComponentFactory;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;

@Service
public final class SelectedGitRepositoryProvider implements IGitRepositorySelectionComponent {

  private final IGitRepositorySelectionComponent selectionComponent;

  public SelectedGitRepositoryProvider(Project project) {
    this.selectionComponent = RuntimeBinding
        .instantiateSoleImplementingClass(IGitRepositorySelectionComponentFactory.class)
        .create(project);
  }

  @Override
  public Option<GitRepository> getSelectedGitRepository() {
    return selectionComponent.getSelectedGitRepository();
  }

  @Override
  public void addSelectionChangeObserver(IGitRepositorySelectionChangeObserver observer) {
    selectionComponent.addSelectionChangeObserver(observer);
  }

  @Override
  public JComponent getSelectionComponent() {
    return selectionComponent.getSelectionComponent();
  }

  public IGitRepositorySelectionProvider getGitRepositorySelectionProvider() {
    return selectionComponent;
  }
}
