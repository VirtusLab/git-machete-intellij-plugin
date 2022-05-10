package com.virtuslab.gitmachete.backend.api;

public interface IGitRebaseParameters {
  IManagedBranchSnapshot getCurrentBranch();

  IManagedBranchSnapshot getNewBaseBranch();

  ICommitOfManagedBranch getForkPointCommit();
}
