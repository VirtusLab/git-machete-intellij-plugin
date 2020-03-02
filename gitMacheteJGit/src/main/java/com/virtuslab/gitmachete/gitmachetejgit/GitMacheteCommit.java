package com.virtuslab.gitmachete.gitmachetejgit;

import lombok.Data;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;

@Data
public class GitMacheteCommit implements IGitMacheteCommit {
	private final String message;
	private final String hash;

	public GitMacheteCommit(IGitCoreCommit coreCommit) {
		message = coreCommit.getMessage();
		hash = coreCommit.getHash().getHashString();
	}
}
