package com.virtuslab.gitmachete.backend.api;

import io.vavr.NotImplementedError;

public interface ILocalBranchReference extends IBranchReference {

  @Override
  default boolean isLocal() {
    return true;
  }

  @Override
  default ILocalBranchReference asLocal() {
    return this;
  }

  @Override
  default IRemoteTrackingBranchReference asRemote() {
    throw new NotImplementedError();
  }

}
