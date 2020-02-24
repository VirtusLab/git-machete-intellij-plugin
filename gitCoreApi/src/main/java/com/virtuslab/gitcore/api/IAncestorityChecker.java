package com.virtuslab.gitcore.api;

public interface IAncestorityChecker {
  /**
   * @return {@code true} if {@code commit} is an ancestor of {@code parentCommit}, otherwise {@code
   *     false}
   */
  boolean check(String commit, String parentCommit) throws GitCoreException;
}
