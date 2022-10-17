package com.virtuslab.gitmachete.backend.api;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

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

  List<ICommitOfManagedBranch> getUniqueCommits();

  List<ICommitOfManagedBranch> getCommitsUntilParent();

  IManagedBranchSnapshot getParent();

  SyncToParentStatus getSyncToParentStatus();

  @Nullable
  IForkPointCommitOfManagedBranch getForkPoint();

  IGitRebaseParameters getParametersForRebaseOntoParent() throws GitMacheteMissingForkPointException;
}
