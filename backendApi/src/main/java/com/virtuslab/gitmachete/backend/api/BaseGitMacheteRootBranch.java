package com.virtuslab.gitmachete.backend.api;

import io.vavr.NotImplementedError;

public abstract class BaseGitMacheteRootBranch extends BaseGitMacheteBranch {
  @Override
  public final boolean isRootBranch() {
    return true;
  }

  @Override
  public final BaseGitMacheteRootBranch asRootBranch() {
    return this;
  }

  @Override
  public final BaseGitMacheteNonRootBranch asNonRootBranch() {
    throw new NotImplementedError();
  }
}
