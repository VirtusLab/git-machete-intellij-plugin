package com.virtuslab.gitmachete.frontend.ui.root;

import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.common.value.qual.MinLen;

import com.virtuslab.gitmachete.frontend.ui.selection.ISelectionChangeObservable;
import com.virtuslab.gitmachete.frontend.ui.selection.ISelectionChangeObserver;

public class VcsRootDropdown extends TextDiffViewerUtil.ComboBoxSettingAction<GitRepository>
    implements
      ISelectionChangeObservable<GitRepository> {
  private final List<GitRepository> repositories;
  private GitRepository selectedRepository;
  private List<ISelectionChangeObserver> observers = List.empty();

  /**
   * @param repositories non-empty list of {@link git4idea.repo.GitRepository} that represents VCS repositories
   */
  public VcsRootDropdown(@MinLen(1) List<GitRepository> repositories) {
    this.repositories = repositories;
    selectedRepository = repositories.get(0);
  }

  @Override
  protected java.util.List<GitRepository> getAvailableOptions() {
    return repositories.asJava();
  }

  @Override
  public GitRepository getValue() {
    return selectedRepository;
  }

  @Override
  protected void setValue(GitRepository option) {
    selectedRepository = option;
    observers.forEach(o -> o.onSelectionChanged());
  }

  @Override
  protected String getText(GitRepository option) {
    return option.getRoot().getName();
  }

  public void addObserver(ISelectionChangeObserver observer) {
    observers = observers.push(observer);
  }

  @NonNegative
  public int getRootCount() {
    return repositories.length();
  }
}
