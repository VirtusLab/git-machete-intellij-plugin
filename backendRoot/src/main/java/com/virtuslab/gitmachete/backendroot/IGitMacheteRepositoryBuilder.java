package com.virtuslab.gitmachete.backendroot;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;

public interface IGitMacheteRepositoryBuilder {
	IGitMacheteRepositoryBuilder repositoryName(String repositoryName);

	IGitMacheteRepositoryBuilder branchRelationFile(IBranchRelationFile branchRelationFile);

	IGitMacheteRepository build() throws GitMacheteException;
}
