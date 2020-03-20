package com.virtuslab.gitmachete.backend.api;

import java.util.Optional;

import io.vavr.collection.List;

public final class NullRepository implements IGitMacheteRepository {
  private static final NullRepository instance = new NullRepository();

  private NullRepository() {}

  public static IGitMacheteRepository getInstance() {
    return instance;
  }

  @Override
  public List<IGitMacheteBranch> getRootBranches() {
    return List.empty();
  }

  @Override
  public Optional<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Optional.empty();
  }

  @Override
  public Optional<IGitMacheteBranch> getBranchByName(String branchName) {
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
  public Optional<IGitMacheteBranch> deriveUpstreamBranch(IGitMacheteBranch branch) {
    return Optional.empty();
  }

  @Override
  public IGitRebaseParameters computeRebaseOntoParentParameters(IGitMacheteBranch branch) {
    return null;
  }

  @Override
  public IGitMergeParameters getMergeIntoParentParameters(IGitMacheteBranch upstreamBranch) {
    return null;
  }
}
