package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;

/**
 * An immutable snapshot of an {@link IGitMacheteRepository} for some specific moment in time.
 * Each {@code get...} method is guaranteed to return the same value each time it's called on a given object.
 */
public interface IGitMacheteRepositorySnapshot {
  IBranchLayout getBranchLayout();

  List<IRootManagedBranchSnapshot> getRootBranches();

  Option<IManagedBranchSnapshot> getCurrentBranchIfManaged();

  List<IManagedBranchSnapshot> getManagedBranches();

  Option<IManagedBranchSnapshot> getManagedBranchByName(String branchName);

  Set<String> getDuplicatedBranchNames();

  Set<String> getSkippedBranchNames();

  Option<IExecutionResult> executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters)
      throws GitMacheteException;

  interface IOngoingRepositoryOperation {
    OngoingRepositoryOperationType getOperationType();

    Option<String> getBaseBranchName();
  }
  IOngoingRepositoryOperation getOngoingRepositoryOperationInfo();
}
