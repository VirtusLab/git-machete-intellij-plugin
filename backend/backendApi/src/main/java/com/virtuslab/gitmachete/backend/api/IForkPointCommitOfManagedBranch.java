package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;

public interface IForkPointCommitOfManagedBranch extends ICommitOfManagedBranch {
  List<IBranchReference> getBranchesContainingInReflog();

  /**
   * @return a subset of {@link #getBranchesContainingInReflog} that skips a reference to each remote tracking branch
   *         such that a reference to its tracked local branch is also included.
   *         For instance, if {@code getBranchesContainingInReflog}
   *         returns {@code [develop, origin/develop, origin/master, fix]},
   *         then this method will return {@code [develop, origin/master, fix]}.
   */
  List<IBranchReference> getUniqueBranchesContainingInReflog();

  boolean isOverridden();
}
