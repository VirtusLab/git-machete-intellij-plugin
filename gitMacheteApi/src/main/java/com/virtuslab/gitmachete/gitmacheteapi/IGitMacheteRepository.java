package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import java.util.List;
import java.util.Optional;

public interface IGitMacheteRepository {
  List<IGitMacheteBranch> getRootBranches();

  void addRootBranch(IGitMacheteBranch branch);

  Optional<IGitMacheteBranch> getCurrentBranch() throws GitMacheteException;

  Optional<String> getRepositoryName();

  List<IGitMacheteSubmoduleEntry> getSubmodules() throws GitMacheteException;

  IBranchRelationFile getMacheteFile();
}
