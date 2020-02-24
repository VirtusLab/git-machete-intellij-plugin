package com.virtuslab.gitcore.api;

import java.util.Date;

public interface IGitCoreCommit {
  String getMessage() throws GitCoreException;

  IGitCorePersonIdentity getAuthor() throws GitCoreException;

  IGitCorePersonIdentity getCommitter() throws GitCoreException;

  Date getCommitTime() throws GitCoreException;

  IGitCoreCommitHash getHash() throws GitCoreException;
}
