package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IGitMacheteRepositorySnapshot {
  Option<IBranchLayout> getBranchLayout();

  List<IGitMacheteRootBranch> getRootBranches();

  Option<IGitMacheteBranch> getCurrentBranchIfManaged();

  List<IGitMacheteBranch> getManagedBranches();

  Option<IGitMacheteBranch> getManagedBranchByName(String branchName);

  Option<Integer> executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters) throws GitMacheteException;
}
