package com.virtuslab.gitmachete.backend.api;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import io.vavr.control.Option;

public interface INonRootManagedBranchSnapshot extends IManagedBranchSnapshot {

  @Override
  default boolean isRoot() {
    return false;
  }

  @Override
  default IRootManagedBranchSnapshot asRoot() {
    throw new NotImplementedError();
  }

  @Override
  default INonRootManagedBranchSnapshot asNonRoot() {
    return this;
  }

  List<ICommitOfManagedBranch> getCommits();

  IManagedBranchSnapshot getParent();

  SyncToParentStatus getSyncToParentStatus();

  Option<IForkPointCommitOfManagedBranch> getForkPoint();

  IGitRebaseParameters getParametersForRebaseOntoParent() throws GitMacheteMissingForkPointException;
}
