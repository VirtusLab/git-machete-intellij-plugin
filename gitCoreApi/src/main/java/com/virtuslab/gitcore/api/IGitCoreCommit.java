package com.virtuslab.gitcore.api;

import java.time.Instant;

/**
 * The only criterion for equality of any instances of any class implementing this interface is equality of {@code getHash}
 */
public interface IGitCoreCommit {
  String getMessage();

  IGitCorePersonIdentity getAuthor();

  IGitCorePersonIdentity getCommitter();

  Instant getCommitTime();

  IGitCoreCommitHash getHash();
}
