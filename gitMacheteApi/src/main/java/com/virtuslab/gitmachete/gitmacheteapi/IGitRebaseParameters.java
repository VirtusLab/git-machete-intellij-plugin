package com.virtuslab.gitmachete.gitmacheteapi;

public interface IGitRebaseParameters {
  IGitMacheteBranch getCurrentBranch();

  IGitMacheteCommit getNewBaseCommit();

  IGitMacheteCommit getForkPointCommit();
}
