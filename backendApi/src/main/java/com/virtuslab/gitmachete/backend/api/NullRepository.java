package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;

public final class NullRepository implements IGitMacheteRepository {
  private static final NullRepository instance = new NullRepository();

  private NullRepository() {}

  public static IGitMacheteRepository getInstance() {
    return instance;
  }

  @Override
  public Option<IBranchLayout> getBranchLayout() {
    return Option.none();
  }

  @Override
  public List<IGitMacheteRootBranch> getRootBranches() {
    return List.empty();
  }

  @Override
  public Option<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Option.none();
  }

  @Override
  public List<IGitMacheteBranch> getManagedBranches() {
    return List.empty();
  }

  @Override
  public Option<IGitMacheteBranch> getManagedBranchByName(String branchName) {
    return Option.none();
  }

  @Override
  public Option<Integer> executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters) {
    return Option.none();
  }
}
