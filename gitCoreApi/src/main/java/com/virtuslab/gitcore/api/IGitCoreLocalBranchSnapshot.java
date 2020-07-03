package com.virtuslab.gitcore.api;

import io.vavr.control.Option;

public interface IGitCoreLocalBranchSnapshot extends IGitCoreBranchSnapshot {

  @Override
  default boolean isLocal() {
    return true;
  }

  Option<IGitCoreRemoteBranchSnapshot> getRemoteTrackingBranch();
}
