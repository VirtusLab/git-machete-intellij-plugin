package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.RequiresQualifier;

import com.virtuslab.qual.gitmachete.backend.api.ConfirmedNonRootBranch;
import com.virtuslab.qual.gitmachete.backend.api.ConfirmedRootBranch;

/**
 * The only criterion for equality of any instances of any class implementing this interface is reference equality
 */
public interface IGitMacheteBranch {
  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedRootBranch.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedNonRootBranch.class)
  boolean isRootBranch();

  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedNonRootBranch.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedRootBranch.class)
  default boolean isNonRootBranch() {
    return !isRootBranch();
  }

  @RequiresQualifier(expression = "this", qualifier = ConfirmedRootBranch.class)
  IGitMacheteRootBranch asRootBranch();

  @RequiresQualifier(expression = "this", qualifier = ConfirmedNonRootBranch.class)
  IGitMacheteNonRootBranch asNonRootBranch();

  String getName();

  IGitMacheteCommit getPointedCommit();

  List<? extends IGitMacheteNonRootBranch> getDownstreamBranches();

  SyncToRemoteStatus getSyncToRemoteStatus();

  Option<String> getCustomAnnotation();

  Option<String> getStatusHookOutput();
}
