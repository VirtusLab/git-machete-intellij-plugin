package com.virtuslab.gitcore.api;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class implementing this interface is equality of
 * {@code derivePointedCommit} and {@code getFullName}
 */
public interface IGitCoreBranch {
  boolean isLocal();

  String getShortName();

  String getFullName();

  IGitCoreCommit derivePointedCommit() throws GitCoreException;

  List<IGitCoreReflogEntry> deriveReflog() throws GitCoreException;

  @EnsuresNonNullIf(expression = "#2", result = true)
  static boolean defaultEquals(IGitCoreBranch self, @Nullable Object other) {
    if (self == other) {
      return true;
    } else if (!(other instanceof IGitCoreBranch)) {
      return false;
    } else {
      var o = (IGitCoreBranch) other;
      return self.getFullName().equals(o.getFullName())
          && Try.of(() -> self.derivePointedCommit().equals(o.derivePointedCommit())).getOrElse(false);
    }
  }

  static int defaultHashCode(IGitCoreBranch self) {
    return self.getFullName().hashCode() * 37 + Try.of(() -> self.derivePointedCommit().hashCode()).getOrElse(0);
  }
}
