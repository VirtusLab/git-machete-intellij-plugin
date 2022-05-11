package com.virtuslab.gitmachete.frontend.graph.api.items;

import io.vavr.NotImplementedError;

import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;

public interface IBranchItem extends IGraphItem {

  IManagedBranchSnapshot getBranch();

  RelationToRemote getRelationToRemote();

  boolean isCurrentBranch();

  // These methods need to be implemented in frontendGraphApi to avoid problems with Subtyping Checker.
  @Override
  default boolean isBranchItem() {
    return true;
  }

  @Override
  default IBranchItem asBranchItem() {
    return this;
  }

  @Override
  default ICommitItem asCommitItem() {
    throw new NotImplementedError();
  }
}
