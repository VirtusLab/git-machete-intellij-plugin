package com.virtuslab.gitmachete.gitmacheteapi;

import java.util.List;
import java.util.Optional;

public interface IGitMacheteRepository {
  List<IGitMacheteBranch> getRootBranches();

  void addRootBranch(IGitMacheteBranch branch);

  Optional<IGitMacheteBranch> getCurrentBranch() throws GitMacheteException;

  Optional<String> getRepositoryName();

  List<IGitMacheteSubmoduleEntry> getSubmodules() throws GitMacheteException;
}
