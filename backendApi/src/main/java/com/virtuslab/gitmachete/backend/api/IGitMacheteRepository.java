package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IGitMacheteRepository {
  Option<IBranchLayout> getBranchLayout();

  List<BaseGitMacheteRootBranch> getRootBranches();

  Option<BaseGitMacheteBranch> getCurrentBranchIfManaged();

  Option<BaseGitMacheteBranch> getBranchByName(String branchName);
}
