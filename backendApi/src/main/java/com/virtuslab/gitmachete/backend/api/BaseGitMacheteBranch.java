package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.RequiresQualifier;

import com.virtuslab.qual.gitmachete.backend.api.ConfirmedNonRootBranch;
import com.virtuslab.qual.gitmachete.backend.api.ConfirmedRootBranch;

/**
 * The only criterion for equality of any instances of any class derived from this one is the reference equality
 */
public abstract class BaseGitMacheteBranch {
  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedRootBranch.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedNonRootBranch.class)
  public abstract boolean isRootBranch();

  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedNonRootBranch.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedRootBranch.class)
  public final boolean isNonRootBranch() {
    return !isRootBranch();
  }

  @RequiresQualifier(expression = "this", qualifier = ConfirmedRootBranch.class)
  public abstract BaseGitMacheteRootBranch asRootBranch();

  @RequiresQualifier(expression = "this", qualifier = ConfirmedNonRootBranch.class)
  public abstract BaseGitMacheteNonRootBranch asNonRootBranch();

  public abstract String getName();

  public abstract IGitMacheteCommit getPointedCommit();

  public abstract List<BaseGitMacheteNonRootBranch> getDownstreamBranches();

  public abstract Option<String> getCustomAnnotation();

  public abstract SyncToRemoteStatus getSyncToRemoteStatus();

  @Override
  public final boolean equals(@Nullable Object other) {
    return this == other;
  }

  @Override
  public final int hashCode() {
    return getName().hashCode() * 37 + getPointedCommit().hashCode();
  }
}
