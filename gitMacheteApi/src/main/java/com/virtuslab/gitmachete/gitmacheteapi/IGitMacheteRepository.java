package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.gitcore.gitcoreapi.GitException;
import java.util.List;
import java.util.Optional;

public interface IGitMacheteRepository {
  List<IGitMacheteBranch> getRootBranches();

  Optional<IGitMacheteBranch> getCurrentBranch() throws GitMacheteException;

  Optional<String> getRepositoryName();

  List<IGitMacheteSubmoduleEntry> getSubmodules() throws GitMacheteException;

  IBranchRelationFile getBranchRelationFile();

  IGitMacheteRepository slideOutBranchWithReinstantiationOfMacheteRepository(String branchName)
      throws GitMacheteException, GitException;

  IGitMacheteRepository slideOutBranchWithReinstantiationOfMacheteRepository(
      IGitMacheteBranch branch) throws GitMacheteException, GitException;
}
