package com.virtuslab.gitcore.api;

public interface IGitCoreRemoteBranch extends IGitCoreBranch {
  @Override
  default boolean isLocal() {
    return false;
  }

  String getRemoteName();
}
