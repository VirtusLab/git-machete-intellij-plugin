package com.virtuslab.gitcore.api;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable snapshot of git repository's HEAD for some specific moment in time.
 * Each {@code get...} method is guaranteed to return the same value each time it's called on a given object.
 */
public interface IGitCoreHeadSnapshot {
  /**
   * @return a branch pointed by HEAD, or null in case of detached HEAD
   */
  @Nullable
  IGitCoreLocalBranchSnapshot getTargetBranch();

  List<IGitCoreReflogEntry> getReflogFromMostRecent();
}
