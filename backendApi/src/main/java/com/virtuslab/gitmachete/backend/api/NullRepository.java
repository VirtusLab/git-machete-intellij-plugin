package com.virtuslab.gitmachete.backend.api;

import java.util.Optional;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;

public final class NullRepository implements IGitMacheteRepository {
  private static final NullRepository instance = new NullRepository();

  private NullRepository() {}

  public static IGitMacheteRepository getInstance() {
    return instance;
  }

  @Override
  public List<BaseGitMacheteRootBranch> getRootBranches() {
    return List.empty();
  }

  @Override
  public Optional<BaseGitMacheteBranch> getCurrentBranchIfManaged() {
    return Optional.empty();
  }

  @Override
  public Optional<BaseGitMacheteBranch> getBranchByName(String branchName) {
    return Optional.empty();
  }

  @Override
  public Optional<String> getRepositoryName() {
    return Optional.empty();
  }

  @Override
  public List<IGitMacheteSubmoduleEntry> getSubmodules() {
    return List.empty();
  }

  @Override
  public IGitRebaseParameters deriveParametersForRebaseOntoParent(BaseGitMacheteNonRootBranch branch) {
    throw new NotImplementedError();
  }

  @Override
  public IGitMergeParameters deriveParametersForMergeIntoParent(BaseGitMacheteNonRootBranch upstreamBranch) {
    throw new NotImplementedError();
  }
}
