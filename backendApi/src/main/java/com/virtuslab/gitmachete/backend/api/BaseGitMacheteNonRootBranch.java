package com.virtuslab.gitmachete.backend.api;

import io.vavr.NotImplementedError;
import io.vavr.collection.List;
import io.vavr.control.Option;

public abstract class BaseGitMacheteNonRootBranch extends BaseGitMacheteBranch {

  @Override
  public final boolean isRootBranch() {
    return false;
  }

  @Override
  public final BaseGitMacheteRootBranch asRootBranch() {
    throw new NotImplementedError();
  }

  @Override
  public final BaseGitMacheteNonRootBranch asNonRootBranch() {
    return this;
  }

  public abstract List<IGitMacheteCommit> getCommits();

  public abstract BaseGitMacheteBranch getUpstreamBranch();

  public abstract SyncToParentStatus getSyncToParentStatus();

  public abstract Option<IGitMacheteCommit> getForkPoint();
}
