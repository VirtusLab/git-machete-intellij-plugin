package com.virtuslab.gitcore.api;

import io.vavr.control.Option;

public interface IGitCoreLocalBranch extends IGitCoreBranch {

  @Override
  default boolean isLocal() {
    return true;
  }

  Option<GitCoreBranchTrackingStatus> deriveRemoteTrackingStatus() throws GitCoreException;

  Option<IGitCoreRemoteBranch> getRemoteTrackingBranch();

  Option<IGitCoreCommit> deriveForkPoint() throws GitCoreException;

  boolean hasJustBeenCreated() throws GitCoreException;
}
