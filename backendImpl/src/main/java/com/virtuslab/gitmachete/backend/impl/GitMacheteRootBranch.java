package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;
import org.checkerframework.dataflow.qual.Pure;

@Getter
@ToString
public final class GitMacheteRootBranch extends BaseGitMacheteRootBranch {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("backend");

  private final String name;
  private final List<GitMacheteNonRootBranch> downstreamBranches;
  private final IGitMacheteCommit pointedCommit;
  private final SyncToRemoteStatus syncToRemoteStatus;
  @Getter(AccessLevel.NONE)
  @Nullable
  private final IGitMacheteRemoteBranch remoteBranch;
  @Nullable
  private final String customAnnotation;

  public GitMacheteRootBranch(String name, List<GitMacheteNonRootBranch> downstreamBranches,
      IGitMacheteCommit pointedCommit,
      SyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation,
      @Nullable IGitMacheteRemoteBranch remoteBranch) {
    LOG.debug(
        () -> "Creating GitMacheteRootBranch(name = ${name}, " +
            "downstreamBranches.length() = ${downstreamBranches.length()}, " +
            "pointedCommit = ${pointedCommit.getHash()}, syncToRemoteStatus = ${syncToRemoteStatus}, " +
            "customAnnotation = ${customAnnotation})");
    this.name = name;
    this.downstreamBranches = downstreamBranches;
    this.pointedCommit = pointedCommit;
    this.syncToRemoteStatus = syncToRemoteStatus;
    this.customAnnotation = customAnnotation;
    this.remoteBranch = remoteBranch;

    // Note: since the class is final, `this` is already @Initialized at this point.

    // This is a hack necessary to create an immutable cyclic structure (children pointing at the parent & parent
    // pointing at the children).
    // This is definitely not the cleanest solution, but still easier to manage and reason about than keeping the
    // upstream data somewhere outside of GitMacheteBranch (e.g. in GitMacheteRepository).
    for (GitMacheteNonRootBranch branch : downstreamBranches) {
      LOG.debug(() -> "Set this (${name}) branch as upstream for ${branch.getName()}");
      branch.setUpstreamBranch(this);
    }
  }

  @Override
  public List<BaseGitMacheteNonRootBranch> getDownstreamBranches() {
    return List.narrow(downstreamBranches);
  }

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }

  @Override
  @Pure
  public Option<IGitMacheteRemoteBranch> getRemoteTrackingBranch() {
    return Option.of(remoteBranch);
  }
}
