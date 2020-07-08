package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.hook.IExecutionResult;

/**
 * An immutable snapshot of an {@link IGitMacheteRepository} for some specific moment in time.
 * Each {@code get...} method is guaranteed to return the same value each time it's called on a given object.
 */
public interface IGitMacheteRepositorySnapshot {
  Option<IBranchLayout> getBranchLayout();

  List<IGitMacheteRootBranch> getRootBranches();

  Option<IGitMacheteBranch> getCurrentBranchIfManaged();

  List<IGitMacheteBranch> getManagedBranches();

  Option<IGitMacheteBranch> getManagedBranchByName(String branchName);

  List<String> getSkippedBranchNames();

  Option<IExecutionResult> executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters)
      throws GitMacheteException;
}
