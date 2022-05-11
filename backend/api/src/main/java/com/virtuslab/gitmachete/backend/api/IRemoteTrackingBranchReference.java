package com.virtuslab.gitmachete.backend.api;

import io.vavr.NotImplementedError;

public interface IRemoteTrackingBranchReference extends IBranchReference {

  @Override
  default boolean isLocal() {
    return false;
  }

  @Override
  default ILocalBranchReference asLocal() {
    throw new NotImplementedError();
  }

  @Override
  default IRemoteTrackingBranchReference asRemote() {
    return this;
  }

  String getRemoteName();

  ILocalBranchReference getTrackedLocalBranch();
}
