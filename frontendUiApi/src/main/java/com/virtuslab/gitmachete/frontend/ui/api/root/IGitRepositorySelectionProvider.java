package com.virtuslab.gitmachete.frontend.ui.api.root;

import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.common.value.qual.MinLen;

public interface IGitRepositorySelectionProvider {
  @UIEffect
  Option<GitRepository> getSelectedRepository();

  void addSelectionChangeObserver(IGitRepositorySelectionChangeObserver observer);

  @UIEffect
  void updateRepositories(@MinLen(1) List<GitRepository> repositories);
}
