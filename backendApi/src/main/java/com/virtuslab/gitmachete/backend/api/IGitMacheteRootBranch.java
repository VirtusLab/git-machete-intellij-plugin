package com.virtuslab.gitmachete.backend.api;

import io.vavr.NotImplementedError;

public interface IGitMacheteRootBranch extends IGitMacheteBranch {
  @Override
  default boolean isRootBranch() {
    return true;
  }

  @Override
  default IGitMacheteRootBranch asRootBranch() {
    return this;
  }

  @Override
  default IGitMacheteNonRootBranch asNonRootBranch() {
    throw new NotImplementedError();
  }
}
