package com.virtuslab.gitcore.api;

import io.vavr.control.Option;

public interface IGitCoreLocalBranch extends IGitCoreBranch {

  @Override
  default boolean isLocal() {
    return true;
  }

  Option<IGitCoreRemoteBranch> getRemoteTrackingBranch();
}
