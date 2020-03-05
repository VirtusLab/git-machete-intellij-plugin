package com.virtuslab.gitmachete.backend.api;

public interface IGitRebaseParameters {
  IGitMacheteBranch getCurrentBranch();

  IGitMacheteCommit getNewBaseCommit();

  IGitMacheteCommit getForkPointCommit();
}
