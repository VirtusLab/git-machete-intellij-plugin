package com.virtuslab.gitcore.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

/**
 * An immutable snapshot of git repository's HEAD for some specific moment in time.
 * Each {@code get...} method is guaranteed to return the same value each time it's called on a given object.
 */
public interface IGitCoreHeadSnapshot {
  /**
   * @return {@link Option.Some} with branch pointed by HEAD, or {@link Option.None} in case of detached HEAD.
   */
  Option<IGitCoreLocalBranchSnapshot> getTargetBranch();

  List<IGitCoreReflogEntry> getReflog();
}
