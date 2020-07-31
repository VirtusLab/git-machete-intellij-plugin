package com.virtuslab.gitcore.api;

public interface IGitCoreRemoteBranchSnapshot extends IGitCoreBranchSnapshot {
  @Override
  default boolean isLocal() {
    return false;
  }

  String getRemoteName();

  /**
   * @return {@code refs/heads/X} for a remote branch {@code refs/remotes/[remote-name]/X};
   *         this is the name of the branch as seen from the perspective of the remote repository
   */
  String getFullNameAsLocalBranchOnRemote();
}
