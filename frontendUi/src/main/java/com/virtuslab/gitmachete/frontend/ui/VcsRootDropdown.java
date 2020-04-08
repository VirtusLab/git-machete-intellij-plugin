package com.virtuslab.gitmachete.frontend.ui;

import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;

public class VcsRootDropdown extends TextDiffViewerUtil.ComboBoxSettingAction<GitRepository> {
  private final List<GitRepository> repositories;
  private GitRepository selectedRepository;
  private List<Runnable> subscribents = List.empty();

  /**
   * @param repositories non-empty list of {@link git4idea.repo.GitRepository} that represents VCS repositories
   */
  @SuppressWarnings("method.invocation.invalid")
  public VcsRootDropdown(List<GitRepository> repositories) {
    this.repositories = repositories;
    assert !repositories.isEmpty() : "List of repositories is empty!";
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
    subscribents.forEach(s -> s.run());
  }

  @Override
  protected String getText(GitRepository option) {
    return option.getRoot().getName();
  }

  public void subscribe(Runnable subscriber) {
    subscribents = subscribents.push(subscriber);
  }

  public int getRootCount() {
    return repositories.length();
  }
}
