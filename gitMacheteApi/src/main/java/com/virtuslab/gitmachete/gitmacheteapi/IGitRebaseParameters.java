package com.virtuslab.gitmachete.gitmacheteapi;

public interface IGitRebaseParameters {
  IGitMacheteBranch getCurrentBranch();

  IGitMacheteBranch getNewBaseBranch();

  IGitMacheteCommit getForkPointCommit();
}
