package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;

public interface IGitMacheteRepositoryBuilder {
  IGitMacheteRepositoryBuilder repositoryName(String repositoryName);

  IGitMacheteRepositoryBuilder branchRelationFile(IBranchRelationFile branchRelationFile);

  IGitMacheteRepository build() throws GitMacheteException;
}
