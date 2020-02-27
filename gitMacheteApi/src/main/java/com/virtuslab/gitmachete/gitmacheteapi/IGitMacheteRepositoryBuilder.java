package com.virtuslab.gitmachete.gitmacheteapi;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;

public interface IGitMacheteRepositoryBuilder {
  IGitMacheteRepositoryBuilder setRepositoryName(String repositoryName);

  IGitMacheteRepositoryBuilder setBranchRelationFile(IBranchRelationFile branchRelationFile);

  IGitMacheteRepository build() throws GitMacheteException;
}
