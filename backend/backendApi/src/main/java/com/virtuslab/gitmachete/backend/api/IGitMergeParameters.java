package com.virtuslab.gitmachete.backend.api;

public interface IGitMergeParameters {
  IManagedBranchSnapshot getCurrentBranch();

  IManagedBranchSnapshot getBranchToMergeInto();
}
