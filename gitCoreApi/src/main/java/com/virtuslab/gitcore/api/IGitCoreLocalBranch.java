package com.virtuslab.gitcore.api;

import io.vavr.control.Option;

public interface IGitCoreLocalBranch extends IGitCoreBranch {
  Option<IGitCoreBranchTrackingStatus> deriveRemoteTrackingStatus() throws GitCoreException;

  Option<IGitCoreRemoteBranch> getRemoteTrackingBranch();

  Option<BaseGitCoreCommit> deriveForkPoint() throws GitCoreException;
}
