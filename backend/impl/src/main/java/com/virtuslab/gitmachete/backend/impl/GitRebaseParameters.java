package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;
import lombok.ToString;

import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;

@Data
public class GitRebaseParameters implements IGitRebaseParameters {
  private final IManagedBranchSnapshot currentBranch;
  private final IBranchReference newBaseBranch;
  private final ICommitOfManagedBranch forkPointCommit;

  @ToString.Include(name = "currentBranch")
  private String getCurrentBranchName() {
    return currentBranch.getName();
  }

  @ToString.Include(name = "newBaseBranch")
  private String getNewBaseBranchName() {
    return newBaseBranch.getName();
  }

  @ToString.Include(name = "forkPointCommit")
  private String getForkPointCommitShortHash() {
    return forkPointCommit.getShortHash();
  }
}
