package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;

/**
 * An immutable snapshot of an {@link IGitMacheteRepository} for some specific moment in time.
 * Each {@code get...} method is guaranteed to return the same value each time it's called on a given object.
 */
public interface IGitMacheteRepositorySnapshot {
  BranchLayout getBranchLayout();

  List<IRootManagedBranchSnapshot> getRootBranches();

  @Nullable
  IManagedBranchSnapshot getCurrentBranchIfManaged();

  /** Branches are ordered as they occur in the machete file */
  List<IManagedBranchSnapshot> getManagedBranches();

  @Nullable
  IManagedBranchSnapshot getManagedBranchByName(String branchName);

  Set<String> getDuplicatedBranchNames();

  Set<String> getSkippedBranchNames();

  @Nullable
  IExecutionResult executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters)
      throws GitMacheteException;

  @Data
  // So that Interning Checker doesn't complain about enum comparison (by `equals` and not by `==`) in Lombok-generated `equals`
  @SuppressWarnings("interning:unnecessary.equals")
  class OngoingRepositoryOperation {
    private final OngoingRepositoryOperationType operationType;

    private final @Nullable String baseBranchName;
  }

  OngoingRepositoryOperation getOngoingRepositoryOperation();
}
