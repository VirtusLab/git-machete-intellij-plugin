package com.virtuslab.gitmachete.backend.api;

public interface IGitMergeParameters {
  IGitMacheteBranch getCurrentBranch();

  IGitMacheteBranch getBranchToMergeInto();
}
