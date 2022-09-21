package com.virtuslab.gitcore.impl.jgit;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreHeadSnapshot;
import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;

@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class GitCoreHeadSnapshot implements IGitCoreHeadSnapshot {

  @ToString.Include
  private final @Nullable IGitCoreLocalBranchSnapshot targetBranch;

  @Getter
  private final List<IGitCoreReflogEntry> reflogFromMostRecent;

  @Override
  public @Nullable IGitCoreLocalBranchSnapshot getTargetBranch() {
    return targetBranch;
  }
}
