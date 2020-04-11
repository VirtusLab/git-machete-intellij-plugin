package com.virtuslab.gitmachete.backend.api;

import java.util.Optional;

import io.vavr.collection.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class derived from this one is the reference equality
 */
public abstract class BaseGitMacheteBranch {
  public abstract boolean isRootBranch();

  public abstract BaseGitMacheteRootBranch asRootBranch();

  public abstract BaseGitMacheteNonRootBranch asNonRootBranch();

  public abstract String getName();

  public abstract IGitMacheteCommit getPointedCommit();

  public abstract List<BaseGitMacheteNonRootBranch> getDownstreamBranches();

  public abstract Optional<String> getCustomAnnotation();

  public abstract ISyncToRemoteStatus getSyncToRemoteStatus();

  @Override
  public final boolean equals(@Nullable Object other) {
    return this == other;
  }

  @Override
  public final int hashCode() {
    return getName().hashCode() * 37 + getPointedCommit().hashCode();
  }
}
