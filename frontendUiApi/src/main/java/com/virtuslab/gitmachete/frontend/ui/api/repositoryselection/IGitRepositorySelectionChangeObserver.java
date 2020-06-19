package com.virtuslab.gitmachete.frontend.ui.api.repositoryselection;

@FunctionalInterface
public interface IGitRepositorySelectionChangeObserver {
  void onSelectionChanged();
}
