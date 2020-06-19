package com.virtuslab.gitmachete.frontend.ui.api.repositoryselection;

import git4idea.repo.GitRepository;
import io.vavr.control.Option;

public interface IGitRepositorySelectionProvider {
  Option<GitRepository> getSelectedRepository();

  void addSelectionChangeObserver(IGitRepositorySelectionChangeObserver observer);
}
