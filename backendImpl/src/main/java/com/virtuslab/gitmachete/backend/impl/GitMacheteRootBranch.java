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
@ToString(callSuper = true)
public final class GitMacheteRootBranch extends BaseGitMacheteBranch implements IGitMacheteRootBranch {

  public GitMacheteRootBranch(
      String name,
      List<GitMacheteNonRootBranch> downstreamBranches,
      IGitMacheteCommit pointedCommit,
      @Nullable IGitMacheteRemoteBranch remoteTrackingBranch,
      SyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation,
      @Nullable String statusHookOutput) {
    super(name, downstreamBranches, pointedCommit, remoteTrackingBranch, syncToRemoteStatus, customAnnotation,
        statusHookOutput);

    LOG.debug("Creating ${this}");

    // Note: since the class is final, `this` is already @Initialized at this point.

    // This is a hack necessary to create an immutable cyclic structure
    // (children pointing at the parent & parent pointing at the children).
    // This is definitely not the cleanest solution, but still easier to manage and reason about than keeping the
    // upstream data somewhere outside of GitMacheteBranch (e.g. in GitMacheteRepositorySnapshot).
    for (GitMacheteNonRootBranch branch : downstreamBranches) {
      branch.setUpstreamBranch(this);
    }
  }
}
