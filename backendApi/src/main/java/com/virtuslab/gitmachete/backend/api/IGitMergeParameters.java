package com.virtuslab.gitmachete.backend.api;

public interface IGitMergeParameters {
  BaseGitMacheteBranch getCurrentBranch();

  BaseGitMacheteBranch getBranchToMergeInto();
}
