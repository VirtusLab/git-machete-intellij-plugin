package com.virtuslab.gitmachete.backend.root;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public interface IGitMacheteRepositoryBuilder {
  IGitMacheteRepositoryBuilder repositoryName(String repositoryName);

  IGitMacheteRepositoryBuilder branchRelationFile(IBranchRelationFile branchRelationFile);

  IGitMacheteRepository build() throws GitMacheteException;
}
