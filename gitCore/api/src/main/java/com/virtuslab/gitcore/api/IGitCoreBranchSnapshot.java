package com.virtuslab.gitcore.api;

import io.vavr.collection.List;
import lombok.val;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable snapshot of a git branch for some specific moment in time.
 * Each {@code get...} method is guaranteed to return the same value each time it's called on a given object.
 *
 * The only criterion for equality of any instances of any class implementing this interface is equality of
 * {@link #getFullName} and {@link #getPointedCommit}
 */
public interface IGitCoreBranchSnapshot {
  boolean isLocal();

  /**
   * @return {@code X} part of {@code refs/heads/X} or {@code [remote-name]/X} part of {@code refs/remotes/[remote-name]/X}
   */
  String getName();

  /**
   * @return {@code refs/heads/X} or {@code refs/remotes/[remote-name]/X}
   */
  String getFullName();

  IGitCoreCommit getPointedCommit();

  List<IGitCoreReflogEntry> getReflogFromMostRecent();

  @EnsuresNonNullIf(expression = "#2", result = true)
  static boolean defaultEquals(@FindDistinct IGitCoreBranchSnapshot self, @Nullable Object other) {
    if (self == other) {
      return true;
    } else if (!(other instanceof IGitCoreBranchSnapshot)) {
      return false;
    } else {
      val o = (IGitCoreBranchSnapshot) other;
      return self.getFullName().equals(o.getFullName())
          && self.getPointedCommit().equals(o.getPointedCommit());
    }
  }

  static int defaultHashCode(IGitCoreBranchSnapshot self) {
    return self.getFullName().hashCode() * 37 + self.getPointedCommit().hashCode();
  }
}
