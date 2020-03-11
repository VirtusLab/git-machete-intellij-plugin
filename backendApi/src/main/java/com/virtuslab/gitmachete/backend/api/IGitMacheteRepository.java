package com.virtuslab.gitmachete.backend.api;

import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface IGitMacheteRepository {
  List<IGitMacheteBranch> getRootBranches();

  Optional<@Nullable IGitMacheteBranch> getCurrentBranchIfManaged() throws GitMacheteException;

  Optional<@Nullable IGitMacheteBranch> getBranchByName(String branchName);

  Optional<@Nullable String> getRepositoryName();

  List<IGitMacheteSubmoduleEntry> getSubmodules();
}
