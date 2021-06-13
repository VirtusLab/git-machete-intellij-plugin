package com.virtuslab.gitcore.api;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.ArrayLen;

/**
 * The only criterion for equality of any instances of any class implementing this interface is equality of
 * {@link #getHashString}
 */
public interface IGitCoreCommitHash {

  @ArrayLen(40)
  String getHashString();

  /**
   * @return hash string abbreviated to the first 7 characters, without a guarantee on being unique within the repository
   */
  @SuppressWarnings({"index:argument", "value:return"})
  @ArrayLen(7)
  default String getShortHashString() {
    return getHashString().substring(0, 7);
  }

  @EnsuresNonNullIf(expression = "#2", result = true)
  static boolean defaultEquals(@FindDistinct IGitCoreCommitHash self, @Nullable Object other) {
    if (self == other) {
      return true;
    } else if (!(other instanceof IGitCoreCommitHash)) {
      return false;
    } else {
      return self.getHashString().equals(((IGitCoreCommitHash) other).getHashString());
    }
  }

  static int defaultHashCode(IGitCoreCommitHash self) {
    return self.getHashString().hashCode();
  }
}
