package com.virtuslab.gitcore.api;

import java.time.Instant;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class implementing this interface is equality of {@link #getHash}
 */
public interface IGitCoreCommit {
  String getShortMessage();

  IGitCorePersonIdentity getAuthor();

  IGitCorePersonIdentity getCommitter();

  Instant getCommitTime();

  IGitCoreCommitHash getHash();

  @EnsuresNonNullIf(expression = "#2", result = true)
  static boolean defaultEquals(@FindDistinct IGitCoreCommit self, @Nullable Object other) {
    if (self == other) {
      return true;
    } else if (!(other instanceof IGitCoreCommit)) {
      return false;
    } else {
      return self.getHash().equals(((IGitCoreCommit) other).getHash());
    }
  }

  static int defaultHashCode(IGitCoreCommit self) {
    return self.getHash().hashCode();
  }
}
