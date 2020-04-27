package com.virtuslab.gitmachete.frontend.ui.api.selection;

@FunctionalInterface
public interface IGitRepositorySelectionChangeObserver {
  void onSelectionChanged();
}
