package com.virtuslab.gitmachete.frontend.graph.api.items;

import io.vavr.NotImplementedError;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public interface IBranchItem extends IGraphItem {

  BaseGitMacheteBranch getBranch();

  SyncToRemoteStatus getSyncToRemoteStatus();

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
