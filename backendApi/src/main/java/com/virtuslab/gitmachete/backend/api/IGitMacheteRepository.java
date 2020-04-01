package com.virtuslab.gitmachete.backend.api;

import java.util.Optional;

import io.vavr.collection.List;

public interface IGitMacheteRepository {
  List<BaseGitMacheteRootBranch> getRootBranches();

  Optional<BaseGitMacheteBranch> getCurrentBranchIfManaged();

  Optional<BaseGitMacheteBranch> getBranchByName(String branchName);

  Optional<String> getRepositoryName();

  List<IGitMacheteSubmoduleEntry> getSubmodules();

  IGitRebaseParameters deriveParametersForRebaseOntoParent(BaseGitMacheteNonRootBranch branch)
      throws GitMacheteException;

  IGitMergeParameters deriveParametersForMergeIntoParent(BaseGitMacheteNonRootBranch upstreamBranch)
      throws GitMacheteException;
}
