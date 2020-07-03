package com.virtuslab.gitcore.impl.jgit;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;

@RequiredArgsConstructor
public abstract class BaseGitCoreBranch implements IGitCoreBranch {

  /**
   * {@code X} part of {@code refs/heads/X} or {@code refs/heads/[remote-name]/X}
   */
  protected final String shortName;

  @Getter
  private final GitCoreCommit pointedCommit;

  @Getter
  private final List<IGitCoreReflogEntry> reflog;

  public abstract String getBranchTypeString(boolean capitalized);

  @Override
  public final boolean equals(@Nullable Object other) {
    return IGitCoreBranch.defaultEquals(this, other);
  }

  @Override
  public final int hashCode() {
    return IGitCoreBranch.defaultHashCode(this);
  }
}
