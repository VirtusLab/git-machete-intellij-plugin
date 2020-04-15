package com.virtuslab.gitmachete.frontend.ui;

import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.common.value.qual.MinLen;

public class VcsRootDropdown extends TextDiffViewerUtil.ComboBoxSettingAction<GitRepository> {
  private final List<GitRepository> repositories;
  private GitRepository selectedRepository;
  private List<Runnable> subscribers = List.empty();

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
    subscribers.forEach(s -> s.run());
  }

  @Override
  protected String getText(GitRepository option) {
    return option.getRoot().getName();
  }

  public void subscribe(Runnable subscriber) {
    subscribers = subscribers.push(subscriber);
  }

  @NonNegative
  public int getRootCount() {
    return repositories.length();
  }
}
