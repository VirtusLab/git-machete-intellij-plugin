package com.virtuslab.gitcore.api;

import java.util.List;
import java.util.Optional;

public interface IGitCoreBranch {
	String getName();

	String getFullName() throws GitCoreException;

	IGitCoreCommit getPointedCommit() throws GitCoreException;

	boolean isLocal();

	List<IGitCoreCommit> computeCommitsUntil(IGitCoreCommit upToCommit) throws GitCoreException;

	boolean hasJustBeenCreated() throws GitCoreException;

	Optional<IGitCoreCommit> computeMergeBase(IGitCoreBranch branch) throws GitCoreException;
}
