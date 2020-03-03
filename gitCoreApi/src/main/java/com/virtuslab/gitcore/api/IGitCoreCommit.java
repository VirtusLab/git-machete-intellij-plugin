package com.virtuslab.gitcore.api;

import java.util.Date;

public interface IGitCoreCommit {
  String getMessage();

  IGitCorePersonIdentity getAuthor();

  IGitCorePersonIdentity getCommitter();

  Date getCommitTime();

  IGitCoreCommitHash getHash();
}
