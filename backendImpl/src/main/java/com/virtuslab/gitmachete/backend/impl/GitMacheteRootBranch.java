package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import lombok.CustomLog;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

@CustomLog
@ToString
public final class GitMacheteRootBranch extends BaseGitMacheteBranch implements IGitMacheteRootBranch {

  public GitMacheteRootBranch(
      String name,
      List<GitMacheteNonRootBranch> childBranches,
      IGitMacheteCommit pointedCommit,
      @Nullable IGitMacheteRemoteBranch remoteTrackingBranch,
      SyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation,
      @Nullable String statusHookOutput) {
    super(name, childBranches, pointedCommit, remoteTrackingBranch, syncToRemoteStatus, customAnnotation, statusHookOutput);

    LOG.debug("Creating ${this}");

    // Note: since the class is final, `this` is already @Initialized at this point.
    setParentForChildBranches();
  }
}
