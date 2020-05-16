package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

@ToString(callSuper = true)
public final class GitMacheteRootBranch extends BaseGitMacheteBranch implements IGitMacheteRootBranch {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("backend");

  public GitMacheteRootBranch(
      String name,
      List<GitMacheteNonRootBranch> downstreamBranches,
      IGitMacheteCommit pointedCommit,
      @Nullable IGitMacheteRemoteBranch remoteBranch,
      SyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation) {
    super(name, downstreamBranches, pointedCommit, remoteBranch, syncToRemoteStatus, customAnnotation);

    LOG.debug(
        () -> "Creating GitMacheteRootBranch(name = ${name}, " +
            "downstreamBranches.length() = ${downstreamBranches.length()}, " +
            "pointedCommit = ${pointedCommit.getHash()}, syncToRemoteStatus = ${syncToRemoteStatus}, " +
            "customAnnotation = ${customAnnotation})");

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
