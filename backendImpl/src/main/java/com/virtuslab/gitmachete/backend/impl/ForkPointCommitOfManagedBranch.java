package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.ToString;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitmachete.backend.api.IForkPointCommitOfManagedBranch;

@ToString
public final class ForkPointCommitOfManagedBranch extends CommitOfManagedBranch implements IForkPointCommitOfManagedBranch {

  @Getter
  private final List<String> branchesContainingInReflog;

  @Getter
  private final boolean isOverridden;

  private ForkPointCommitOfManagedBranch(IGitCoreCommit coreCommit, List<String> branchesContainingInReflog,
      boolean isOverridden) {
    super(coreCommit);
    this.branchesContainingInReflog = branchesContainingInReflog;
    this.isOverridden = isOverridden;
  }

  public static ForkPointCommitOfManagedBranch overridden(IGitCoreCommit overrideCoreCommit) {
    return new ForkPointCommitOfManagedBranch(overrideCoreCommit, List.empty(), true);
  }

  public static ForkPointCommitOfManagedBranch inferred(IGitCoreCommit coreCommit, List<String> branchesContainingInReflog) {
    return new ForkPointCommitOfManagedBranch(coreCommit, branchesContainingInReflog, false);
  }

  public static ForkPointCommitOfManagedBranch fallbackToParent(IGitCoreCommit parentCoreCommit) {
    return new ForkPointCommitOfManagedBranch(parentCoreCommit, List.empty(), false);
  }
}
