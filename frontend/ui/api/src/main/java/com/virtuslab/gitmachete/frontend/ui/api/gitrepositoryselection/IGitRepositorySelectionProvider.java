package com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection;

import git4idea.repo.GitRepository;
import io.vavr.control.Option;

public interface IGitRepositorySelectionProvider {
  Option<GitRepository> getSelectedGitRepository();

  void addSelectionChangeObserver(IGitRepositorySelectionChangeObserver observer);
}
