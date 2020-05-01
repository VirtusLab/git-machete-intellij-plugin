package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IGitMacheteRepository {
  List<BaseGitMacheteRootBranch> getRootBranches();

  Option<BaseGitMacheteBranch> getCurrentBranchIfManaged();

  Option<BaseGitMacheteBranch> getBranchByName(String branchName);
}
