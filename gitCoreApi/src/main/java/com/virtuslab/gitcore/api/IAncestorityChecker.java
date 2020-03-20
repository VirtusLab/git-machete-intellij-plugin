package com.virtuslab.gitcore.api;

public interface IAncestorityChecker {
  boolean isAncestor(BaseGitCoreCommit presumedAncestor, BaseGitCoreCommit presumedDescendant)
      throws GitCoreException;
}
