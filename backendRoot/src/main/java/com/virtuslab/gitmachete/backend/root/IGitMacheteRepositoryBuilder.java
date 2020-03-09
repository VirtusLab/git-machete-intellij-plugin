package com.virtuslab.gitmachete.backend.root;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public interface IGitMacheteRepositoryBuilder {
  IGitMacheteRepositoryBuilder repositoryName(String repositoryName);

  IGitMacheteRepositoryBuilder branchLayout(IBranchLayout branchLayout);

  IGitMacheteRepository build() throws GitMacheteException;
}
