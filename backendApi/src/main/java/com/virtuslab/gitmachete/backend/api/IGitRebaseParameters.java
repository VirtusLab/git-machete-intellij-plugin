package com.virtuslab.gitmachete.backend.api;

public interface IGitRebaseParameters {
  IManagedBranchSnapshot getCurrentBranch();

  ICommitOfManagedBranch getNewBaseCommit();

  ICommitOfManagedBranch getForkPointCommit();
}
