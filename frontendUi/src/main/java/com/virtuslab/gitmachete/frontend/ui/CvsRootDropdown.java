package com.virtuslab.gitmachete.frontend.ui;

import java.util.function.Consumer;

import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import com.intellij.dvcs.repo.Repository;
import io.vavr.collection.List;

public class CvsRootDropdown extends TextDiffViewerUtil.ComboBoxSettingAction<Repository> {
  private final List<Repository> repositories;
  private Repository selectedRepository;
  private List<Consumer<Repository>> subscribents = List.empty();

  @SuppressWarnings("method.invocation.invalid")
  public CvsRootDropdown(List<Repository> repositories) {
    this.repositories = repositories;
    assert !repositories.isEmpty() : "List of repositories is empty!";
    selectedRepository = repositories.get(0);

    setPopupTitle("Repositories");
  }

  @Override
  protected java.util.List<Repository> getAvailableOptions() {
    return repositories.asJava();
  }

  @Override
  public Repository getValue() {
    return selectedRepository;
  }

  @Override
  protected void setValue(Repository option) {
    selectedRepository = option;
    subscribents.forEach(s -> s.accept(selectedRepository));
  }

  @Override
  protected String getText(Repository option) {
    return option.getRoot().getName();
  }

  public void subscribe(Consumer<Repository> subscriber) {
    subscribents = subscribents.push(subscriber);
  }
}
