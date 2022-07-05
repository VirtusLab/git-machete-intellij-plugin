package com.virtuslab.gitmachete.backend.api;

import java.time.Instant;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.ArrayLen;

/**
 * The only criterion for equality of any instances of any class implementing this interface is equality of {@link #getHash}
 */
public interface ICommitOfManagedBranch {
  String getShortMessage();

  String getFullMessage();

  @ArrayLen(40)
  String getHash();

  @ArrayLen(7)
  String getShortHash();

  Instant getCommitTime();

  @EnsuresNonNullIf(expression = "#2", result = true)
  static boolean defaultEquals(@FindDistinct ICommitOfManagedBranch self, @Nullable Object other) {
    if (self == other) {
      return true;
    } else if (!(other instanceof ICommitOfManagedBranch)) {
      return false;
    } else {
      return self.getHash().equals(((ICommitOfManagedBranch) other).getHash());
    }
  }

  static int defaultHashCode(ICommitOfManagedBranch self) {
    return self.getHash().hashCode();
  }
}
