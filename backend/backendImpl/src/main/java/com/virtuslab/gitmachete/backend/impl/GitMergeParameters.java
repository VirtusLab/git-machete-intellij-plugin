package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;

import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;

@Data
public class GitMergeParameters implements IGitMergeParameters {
  private final IManagedBranchSnapshot currentBranch;
  private final IManagedBranchSnapshot branchToMergeInto;
}
