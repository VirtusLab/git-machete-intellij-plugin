package com.virtuslab.gitcore.impl.jgit;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;

@RequiredArgsConstructor
public abstract class BaseGitCoreBranchSnapshot implements IGitCoreBranchSnapshot {

  /**
   * {@code X} part of {@code refs/heads/X} or {@code refs/heads/[remote-name]/X}
   */
  protected final String shortName;

  @Getter
  private final GitCoreCommit pointedCommit;

  @Getter
  private final List<IGitCoreReflogEntry> reflogFromMostRecent;

  public abstract String getBranchTypeString(boolean capitalized);

  @Override
  public final boolean equals(@Nullable Object other) {
    return IGitCoreBranchSnapshot.defaultEquals(this, other);
  }

  @Override
  public final int hashCode() {
    return IGitCoreBranchSnapshot.defaultHashCode(this);
  }
}
