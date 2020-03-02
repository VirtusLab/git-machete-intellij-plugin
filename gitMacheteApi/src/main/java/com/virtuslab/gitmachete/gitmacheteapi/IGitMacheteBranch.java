package com.virtuslab.gitmachete.gitmacheteapi;

import java.util.List;
import java.util.Optional;

public interface IGitMacheteBranch {
	String getName();

	List<IGitMacheteCommit> computeCommits() throws GitMacheteException;

	IGitMacheteCommit getPointedCommit() throws GitMacheteException;

	List<IGitMacheteBranch> getDownstreamBranches();

	Optional<String> getCustomAnnotation();

	Optional<IGitMacheteBranch> getUpstreamBranch();

	SyncToParentStatus computeSyncToParentStatus() throws GitMacheteException;

	SyncToOriginStatus computeSyncToOriginStatus() throws GitMacheteException;

	IGitRebaseParameters computeRebaseParameters() throws GitMacheteException;

	IGitMergeParameters getMergeParameters() throws GitMacheteException;
}
