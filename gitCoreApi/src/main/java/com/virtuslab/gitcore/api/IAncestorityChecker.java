package com.virtuslab.gitcore.api;

public interface IAncestorityChecker {
	boolean isAncestor(IGitCoreCommitHash presumedAncestor, IGitCoreCommitHash presumedDescendant)
			throws GitCoreException;
}
