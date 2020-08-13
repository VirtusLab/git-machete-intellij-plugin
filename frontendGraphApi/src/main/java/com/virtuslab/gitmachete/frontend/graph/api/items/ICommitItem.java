package com.virtuslab.gitmachete.frontend.graph.api.items;

import io.vavr.NotImplementedError;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;

public interface ICommitItem extends IGraphItem {
  // These methods need to be implemented in frontendGraphApi (as opposed to an implementation subproject)
  // to avoid problems with Subtyping Checker.
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

  ICommitOfManagedBranch getCommit();

  /**
   * @return a non-root branch containing the given commit in git-machete sense, which is stricter than containing in git sense.
   *
   *         <p><b>Branch A "contains" commit X in git sense</b> means that
   *         there's a path (possibly of zero length) from the commit pointed by A to commit X.</p>
   *
   *         <p><b>Branch A "contains" commit X in git-machete sense</b> means that
   *         A is a non-root branch and X lies between commit pointed by A and A's fork point.</p>
   */
  INonRootManagedBranchSnapshot getContainingBranch();
}
