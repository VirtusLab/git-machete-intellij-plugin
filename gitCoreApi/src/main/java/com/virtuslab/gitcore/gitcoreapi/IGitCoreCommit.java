package com.virtuslab.gitcore.gitcoreapi;

import java.util.Date;

public interface IGitCoreCommit {
  String getMessage() throws GitException;

  IGitCorePersonIdentity getAuthor() throws GitException;

  IGitCorePersonIdentity getCommitter() throws GitException;

  Date getCommitTime() throws GitException;

  IGitCoreCommitHash getHash() throws GitException;

  boolean isAncestorOf(IGitCoreCommit parentCommit) throws GitException;
}
