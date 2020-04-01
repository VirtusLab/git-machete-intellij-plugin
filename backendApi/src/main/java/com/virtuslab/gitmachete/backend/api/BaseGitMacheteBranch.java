package com.virtuslab.gitmachete.backend.api;

import java.util.Optional;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class derived from this one is the reference equality
 */
public abstract class BaseGitMacheteBranch {
  public abstract String getName();

  public abstract List<IGitMacheteCommit> getCommits();

  public abstract IGitMacheteCommit getPointedCommit();

  public abstract Optional<BaseGitMacheteBranch> getUpstreamBranch();

  public abstract List<BaseGitMacheteBranch> getDownstreamBranches();

  public abstract Optional<String> getCustomAnnotation();

  public abstract SyncToParentStatus getSyncToParentStatus();

  public abstract SyncToOriginStatus getSyncToOriginStatus();

  public abstract Optional<IGitMacheteCommit> deriveForkPoint() throws GitMacheteException;

  @Override
  public final boolean equals(@Nullable Object other) {
    return this == other;
  }

  @Override
  public final int hashCode() {
    return getName().hashCode()
        + getPointedCommit().getHash().hashCode()
        + getSyncToOriginStatus().hashCode()
        + getSyncToParentStatus().hashCode();
  }
}
