package com.virtuslab.gitcore.api;

import java.util.Optional;

public interface IGitCoreLocalBranch extends IGitCoreBranch {
	Optional<IGitCoreBranchTrackingStatus> computeRemoteTrackingStatus() throws GitCoreException;

	Optional<IGitCoreRemoteBranch> getRemoteTrackingBranch() throws GitCoreException;

	Optional<IGitCoreCommit> computeForkPoint() throws GitCoreException;
}
