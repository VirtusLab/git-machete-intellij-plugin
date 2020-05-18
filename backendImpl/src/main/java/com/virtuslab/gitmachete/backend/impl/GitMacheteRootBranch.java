package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.logger.EnhancedLambdaLoggerFactory;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@ToString(callSuper = true)
public final class GitMacheteRootBranch extends BaseGitMacheteBranch implements IGitMacheteRootBranch {
  private static final IEnhancedLambdaLogger LOG = EnhancedLambdaLoggerFactory.create();

  public GitMacheteRootBranch(
      String name,
      List<GitMacheteNonRootBranch> downstreamBranches,
      IGitMacheteCommit pointedCommit,
      @Nullable IGitMacheteRemoteBranch remoteBranch,
      SyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation,
      @Nullable String statusHookOutput) {
    super(name, downstreamBranches, pointedCommit, remoteBranch, syncToRemoteStatus, customAnnotation, statusHookOutput);

    LOG.debug("Creating ${this}");

    // Note: since the class is final, `this` is already @Initialized at this point.

    // This is a hack necessary to create an immutable cyclic structure (children pointing at the parent & parent
    // pointing at the children).
    // This is definitely not the cleanest solution, but still easier to manage and reason about than keeping the
    // upstream data somewhere outside of GitMacheteBranch (e.g. in GitMacheteRepository).
    for (GitMacheteNonRootBranch branch : downstreamBranches) {
      branch.setUpstreamBranch(this);
    }
  }
}
