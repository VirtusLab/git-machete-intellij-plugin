package com.virtuslab.gitmachete.frontend.ui.selection;

public interface ISelectionChangeObservable<T> {
  T getValue();

  void addObserver(ISelectionChangeObserver observer);
}
