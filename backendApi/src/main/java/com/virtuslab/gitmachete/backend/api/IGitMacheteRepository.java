package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IGitMacheteRepository {
  Option<IBranchLayout> getBranchLayout();

  List<IGitMacheteRootBranch> getRootBranches();

  Option<IGitMacheteBranch> getCurrentBranchIfManaged();

  Option<IGitMacheteBranch> getBranchByName(String branchName);

  Option<Integer> executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters) throws GitMacheteException;
}
