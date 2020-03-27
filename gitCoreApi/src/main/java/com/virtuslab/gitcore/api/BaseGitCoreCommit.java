package com.virtuslab.gitcore.api;

import java.util.Date;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class derived from this one is the equality of
 * {@code getHash}
 */
public abstract class BaseGitCoreCommit {
  public abstract String getMessage();

  public abstract IGitCorePersonIdentity getAuthor();

  public abstract IGitCorePersonIdentity getCommitter();

  public abstract Date getCommitTime();

  public abstract BaseGitCoreCommitHash getHash();

  @Override
  public final boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof BaseGitCoreCommit)) {
      return false;
    } else {
      return getHash().equals(((BaseGitCoreCommit) other).getHash());
    }
  }

  @Override
  public int hashCode() {
    return getHash().hashCode();
  }
}
