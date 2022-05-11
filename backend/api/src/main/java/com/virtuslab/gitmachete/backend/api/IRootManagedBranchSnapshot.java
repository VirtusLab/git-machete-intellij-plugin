package com.virtuslab.gitmachete.backend.api;

import io.vavr.NotImplementedError;

public interface IRootManagedBranchSnapshot extends IManagedBranchSnapshot {
  @Override
  default boolean isRoot() {
    return true;
  }

  @Override
  default IRootManagedBranchSnapshot asRoot() {
    return this;
  }

  @Override
  default INonRootManagedBranchSnapshot asNonRoot() {
    throw new NotImplementedError();
  }
}
