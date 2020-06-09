package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;
import lombok.ToString;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

@Data
public class GitRebaseParameters implements IGitRebaseParameters {
  private final IGitMacheteBranch currentBranch;
  private final IGitMacheteCommit newBaseCommit;
  private final IGitMacheteCommit forkPointCommit;

  @ToString.Include(name = "currentBranch")
  private String getCurrentBranchBame() {
    return currentBranch.getName();
  }

  @ToString.Include(name = "newBaseCommit")
  private String getNewBaseCommitShortHash() {
    return newBaseCommit.getShortHash();
  }

  @ToString.Include(name = "forkPointCommit")
  private String getForkPointCommitShortHash() {
    return forkPointCommit.getShortHash();
  }
}
