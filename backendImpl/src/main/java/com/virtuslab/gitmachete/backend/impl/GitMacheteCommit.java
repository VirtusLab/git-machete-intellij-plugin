package com.virtuslab.gitmachete.backend.impl;

import java.time.Instant;

import lombok.Data;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;

@Data
public class GitMacheteCommit implements IGitMacheteCommit {
  private final String message;
  private final String hash;
  private final Instant commitTime;

  public GitMacheteCommit(BaseGitCoreCommit coreCommit) {
    message = coreCommit.getMessage();
    hash = coreCommit.getHash().getHashString();
    commitTime = coreCommit.getCommitTime();
  }
}
