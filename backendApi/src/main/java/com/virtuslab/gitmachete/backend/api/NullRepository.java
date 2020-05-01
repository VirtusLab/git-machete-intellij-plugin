package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.control.Option;

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
  public Option<BaseGitMacheteBranch> getCurrentBranchIfManaged() {
    return Option.none();
  }

  @Override
  public Option<BaseGitMacheteBranch> getBranchByName(String branchName) {
    return Option.none();
  }
}
