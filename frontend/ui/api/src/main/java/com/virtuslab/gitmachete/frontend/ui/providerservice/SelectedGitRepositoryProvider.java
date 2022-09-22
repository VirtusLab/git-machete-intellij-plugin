package com.virtuslab.gitmachete.frontend.ui.providerservice;

import javax.swing.JComponent;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionChangeObserver;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionComponentProvider;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionComponentProviderFactory;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;

@Service
public final class SelectedGitRepositoryProvider implements IGitRepositorySelectionComponentProvider {

  private final IGitRepositorySelectionComponentProvider selectionComponent;

  public SelectedGitRepositoryProvider(Project project) {
    this.selectionComponent = RuntimeBinding
        .instantiateSoleImplementingClass(IGitRepositorySelectionComponentProviderFactory.class)
        .create(project);
  }

  @Override
  public @Nullable GitRepository getSelectedGitRepository() {
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
