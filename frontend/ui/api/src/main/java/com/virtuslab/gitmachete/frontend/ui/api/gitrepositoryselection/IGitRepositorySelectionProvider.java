package com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection;

import git4idea.repo.GitRepository;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface IGitRepositorySelectionProvider {
  @Nullable
  GitRepository getSelectedGitRepository();

  void addSelectionChangeObserver(IGitRepositorySelectionChangeObserver observer);
}
