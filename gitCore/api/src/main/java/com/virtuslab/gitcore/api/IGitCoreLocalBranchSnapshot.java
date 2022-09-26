package com.virtuslab.gitcore.api;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface IGitCoreLocalBranchSnapshot extends IGitCoreBranchSnapshot {

  @Override
  default boolean isLocal() {
    return true;
  }

  @Nullable
  IGitCoreRemoteBranchSnapshot getRemoteTrackingBranch();
}
