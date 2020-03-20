package com.virtuslab.gitmachete.backend.api;

import java.util.Optional;

import io.vavr.collection.List;

public interface IGitMacheteRepository {
  List<IGitMacheteBranch> getRootBranches();

  Optional<IGitMacheteBranch> getCurrentBranchIfManaged();

  Optional<IGitMacheteBranch> getBranchByName(String branchName);

  Optional<String> getRepositoryName();

  List<IGitMacheteSubmoduleEntry> getSubmodules();

  Optional<IGitMacheteBranch> deriveExistingUpstreamBranch(IGitMacheteBranch branch);

  IGitRebaseParameters deriveParametersForRebaseOntoParent(IGitMacheteBranch branch) throws GitMacheteException;

  IGitMergeParameters deriveParametersForMergeIntoParent(IGitMacheteBranch upstreamBranch) throws GitMacheteException;
}
