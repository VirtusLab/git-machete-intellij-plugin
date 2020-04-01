package com.virtuslab.gitmachete.backend.api;

import java.util.Optional;

import io.vavr.collection.List;

public interface IGitMacheteRepository {
  List<BaseGitMacheteBranch> getRootBranches();

  Optional<BaseGitMacheteBranch> getCurrentBranchIfManaged();

  Optional<BaseGitMacheteBranch> getBranchByName(String branchName);

  Optional<String> getRepositoryName();

  List<IGitMacheteSubmoduleEntry> getSubmodules();

  IGitRebaseParameters deriveParametersForRebaseOntoParent(BaseGitMacheteBranch branch) throws GitMacheteException;

  IGitMergeParameters deriveParametersForMergeIntoParent(BaseGitMacheteBranch upstreamBranch)
      throws GitMacheteException;
}
