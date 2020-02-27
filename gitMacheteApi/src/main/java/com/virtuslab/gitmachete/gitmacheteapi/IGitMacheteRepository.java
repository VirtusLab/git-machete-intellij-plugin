package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import java.util.List;
import java.util.Optional;

public interface IGitMacheteRepository {
  List<IGitMacheteBranch> getRootBranches();

  Optional<IGitMacheteBranch> getCurrentBranchIfManaged() throws GitMacheteException;

  Optional<IGitMacheteBranch> getBranchByName(String branchName);

  Optional<String> getRepositoryName();

  List<IGitMacheteSubmoduleEntry> getSubmodules() throws GitMacheteException;

  IBranchRelationFile getBranchRelationFile();
}
