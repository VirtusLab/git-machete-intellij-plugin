package com.virtuslab.gitmachete.backend.api;

public interface IGitRebaseParameters {
  BaseGitMacheteBranch getCurrentBranch();

  IGitMacheteCommit getNewBaseCommit();

  IGitMacheteCommit getForkPointCommit();
}
