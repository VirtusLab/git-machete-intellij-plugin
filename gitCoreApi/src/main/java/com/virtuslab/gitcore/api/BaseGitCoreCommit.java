package com.virtuslab.gitcore.api;

import java.util.Date;

import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BaseGitCoreCommit {
  public abstract String getMessage();

  public abstract IGitCorePersonIdentity getAuthor();

  public abstract IGitCorePersonIdentity getCommitter();

  public abstract Date getCommitTime();

  public abstract String getHashString();

  @Override
  public final boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof BaseGitCoreCommit)) {
      return false;
    } else {
      return getHashString().equals(((BaseGitCoreCommit) other).getHashString());
    }
  }

  @Override
  public int hashCode() {
    return getHashString().hashCode();
  }
}
