package com.virtuslab.gitcore.api;

import io.vavr.control.Option;

public interface IGitCoreLocalBranch extends IGitCoreBranch {
  Option<GitCoreBranchTrackingStatus> deriveRemoteTrackingStatus() throws GitCoreException;

  Option<IGitCoreRemoteBranch> getRemoteTrackingBranch();

  Option<IGitCoreCommit> deriveForkPoint() throws GitCoreException;
}
