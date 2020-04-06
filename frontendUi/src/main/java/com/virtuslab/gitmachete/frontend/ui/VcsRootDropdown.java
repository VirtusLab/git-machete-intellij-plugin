package com.virtuslab.gitmachete.frontend.ui;

import java.util.function.Consumer;

import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;

public class VcsRootDropdown extends TextDiffViewerUtil.ComboBoxSettingAction<GitRepository> {
  private final List<GitRepository> repositories;
  private GitRepository selectedRepository;
  private List<Consumer<GitRepository>> subscribents = List.empty();

  @SuppressWarnings("method.invocation.invalid")
  public VcsRootDropdown(List<GitRepository> repositories) {
    this.repositories = repositories;
    assert !repositories.isEmpty() : "List of repositories is empty!";
    selectedRepository = repositories.get(0);

    setPopupTitle("Repositories");
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
    subscribents.forEach(s -> s.accept(selectedRepository));
  }

  @Override
  protected String getText(GitRepository option) {
    return option.getRoot().getName();
  }

  public void subscribe(Consumer<GitRepository> subscriber) {
    subscribents = subscribents.push(subscriber);
  }

  public int getRootCount() {
    return repositories.length();
  }
}
