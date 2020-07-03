package com.virtuslab.gitcore.api;

public interface IGitCoreRemoteBranchSnapshot extends IGitCoreBranchSnapshot {
  @Override
  default boolean isLocal() {
    return false;
  }

  String getRemoteName();
}
