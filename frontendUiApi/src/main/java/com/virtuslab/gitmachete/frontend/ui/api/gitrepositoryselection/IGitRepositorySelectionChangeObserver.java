package com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection;

@FunctionalInterface
public interface IGitRepositorySelectionChangeObserver {
  void onSelectionChanged();
}
