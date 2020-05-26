package com.virtuslab.gitmachete.frontend.graph.api.items;

import io.vavr.NotImplementedError;

import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;

public interface ICommitItem extends IGraphItem {
  // These methods need to be implemented in frontendGraphApi to avoid problems with Subtyping Checker.
  @Override
  default boolean isBranchItem() {
    return false;
  }

  @Override
  default IBranchItem asBranchItem() {
    throw new NotImplementedError();
  }

  @Override
  default ICommitItem asCommitItem() {
    return this;
  }

  IGitMacheteCommit getCommit();
  IGitMacheteNonRootBranch getContainingBranch();
}
