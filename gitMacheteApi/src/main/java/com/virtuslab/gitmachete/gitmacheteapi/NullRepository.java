package com.virtuslab.gitmachete.gitmacheteapi;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class NullRepository implements IGitMacheteRepository {
  private static NullRepository instance = new NullRepository();

  private NullRepository() {
  }

  public static IGitMacheteRepository getInstance() {
    return instance;
  }

  @Override
  public List<IGitMacheteBranch> getRootBranches() {
    return Collections.emptyList();
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
    return Collections.emptyList();
  }
}
