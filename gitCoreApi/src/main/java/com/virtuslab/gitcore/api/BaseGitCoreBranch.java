package com.virtuslab.gitcore.api;

import io.vavr.control.Try;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criteria for equality of any instances of any class derived from this one is the equality of
 * {@code getFullName} and {@code getPointedCommit}
 */
public abstract class BaseGitCoreBranch implements IGitCoreBranch {

  @Override
  public final boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof BaseGitCoreBranch)) {
      return false;
    } else {
      var o = (BaseGitCoreBranch) other;
      return getFullName().equals(o.getFullName())
          && Try.of(() -> getPointedCommit().equals(o.getPointedCommit())).getOrElse(false);
    }
  }

  @Override
  public int hashCode() {
    return getFullName().hashCode() * 37 + Try.of(() -> getPointedCommit().hashCode()).getOrElse(0);
  }
}
