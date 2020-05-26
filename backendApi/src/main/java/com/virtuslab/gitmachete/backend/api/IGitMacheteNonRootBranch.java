package com.virtuslab.gitmachete.backend.api;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IGitMacheteNonRootBranch extends IGitMacheteBranch {

  @Override
  default boolean isRootBranch() {
    return false;
  }

  @Override
  default IGitMacheteRootBranch asRootBranch() {
    throw new NotImplementedError();
  }

  @Override
  default IGitMacheteNonRootBranch asNonRootBranch() {
    return this;
  }

  List<IGitMacheteCommit> getCommits();

  IGitMacheteBranch getUpstreamBranch();

  SyncToParentStatus getSyncToParentStatus();

  Option<IGitMacheteForkPointCommit> getForkPoint();

  IGitRebaseParameters getParametersForRebaseOntoParent() throws GitMacheteMissingForkPointException;

  IGitMergeParameters getParametersForMergeIntoParent();
}
