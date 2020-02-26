package com.virtuslab.gitcore.impl.jgit;

import com.virtuslab.gitcore.api.IGitCoreCommitHash;

public class GitCoreCommitHash extends GitCoreObjectHash implements IGitCoreCommitHash {
  public GitCoreCommitHash(String hashString) {
    super(hashString);
  }
}
