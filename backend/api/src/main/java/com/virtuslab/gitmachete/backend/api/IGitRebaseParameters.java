package com.virtuslab.gitmachete.backend.api;

public interface IGitRebaseParameters {
  IManagedBranchSnapshot getCurrentBranch();

  IBranchReference getNewBaseBranch();

  ICommitOfManagedBranch getForkPointCommit();
}
