package com.virtuslab.gitcore.api;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class derived from this one is the equality of
 * {@code getHashString}
 */
public abstract class BaseGitCoreCommitHash {
  public abstract String getHashString();

  @Override
  public final boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof BaseGitCoreCommitHash)) {
      return false;
    } else {
      return getHashString().equals(((BaseGitCoreCommitHash) other).getHashString());
    }
  }

  @Override
  public final int hashCode() {
    return getHashString().hashCode();
  }
}
