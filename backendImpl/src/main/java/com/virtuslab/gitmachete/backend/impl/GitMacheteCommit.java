package com.virtuslab.gitmachete.backend.impl;

import lombok.Data;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;

@Data
public class GitMacheteCommit implements IGitMacheteCommit {
  private final String message;
  private final String hash;

  public GitMacheteCommit(BaseGitCoreCommit coreCommit) {
    message = coreCommit.getMessage();
    hash = coreCommit.getHash().getHashString();
  }
}
