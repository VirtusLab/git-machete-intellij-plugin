package com.virtuslab.gitmachete.gitmachetejgit;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteSubmoduleEntry;

@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {
	private final String repositoryName;

	@Getter
	private final List<IGitMacheteBranch> rootBranches;
	@Getter
	private final List<IGitMacheteSubmoduleEntry> submodules;

	@Getter
	private final IBranchRelationFile branchRelationFile;

	private final IGitMacheteBranch currentBranch;

	private final Map<String, IGitMacheteBranch> branchByName;

	@Override
	public Optional<String> getRepositoryName() {
		return Optional.ofNullable(repositoryName);
	}

	@Override
	public Optional<IGitMacheteBranch> getCurrentBranchIfManaged() {
		return Optional.ofNullable(currentBranch);
	}

	@Override
	public Optional<IGitMacheteBranch> getBranchByName(String branchName) {
		return Optional.ofNullable(branchByName.getOrDefault(branchName, null));
	}
}
