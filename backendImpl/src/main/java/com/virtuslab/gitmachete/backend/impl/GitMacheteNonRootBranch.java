package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteMissingForkPointException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

@Getter
@ToString
public final class GitMacheteNonRootBranch extends BaseGitMacheteNonRootBranch {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("backend");

  private final String name;
  @ToString.Exclude
  @MonotonicNonNull
  private BaseGitMacheteBranch upstreamBranch = null;
  private final List<GitMacheteNonRootBranch> downstreamBranches;
  @Nullable
  private final IGitMacheteCommit forkPoint;
  private final IGitMacheteCommit pointedCommit;
  private final List<IGitMacheteCommit> commits;
  private final SyncToRemoteStatus syncToRemoteStatus;
  private final SyncToParentStatus syncToParentStatus;
  @Nullable
  private final IGitMacheteRemoteBranch remoteBranch;
  @Nullable
  private final String customAnnotation;

  public GitMacheteNonRootBranch(String name,
      List<GitMacheteNonRootBranch> downstreamBranches,
      @Nullable IGitMacheteCommit forkPoint,
      IGitMacheteCommit pointedCommit,
      List<IGitMacheteCommit> commits,
      SyncToRemoteStatus syncToRemoteStatus,
      SyncToParentStatus syncToParentStatus,
      @Nullable String customAnnotation,
      @Nullable IGitMacheteRemoteBranch remoteBranch) {
    LOG.debug(
        () -> "Creating GitMacheteNonRootBranch(name = ${name}, downstreamBranches.length() = ${downstreamBranches.length()}, "
            + "forkPoint = ${forkPoint != null ? forkPoint.getHash() : null}, pointedCommit = ${pointedCommit.getHash()}, "
            + "commits.length() = ${commits.length()}, syncToRemoteStatus = ${syncToRemoteStatus}, "
            + "syncToParentStatus = ${syncToParentStatus}, customAnnotation = ${customAnnotation})");
    this.name = name;
    this.downstreamBranches = downstreamBranches;
    this.forkPoint = forkPoint;
    this.pointedCommit = pointedCommit;
    this.commits = commits;
    this.syncToRemoteStatus = syncToRemoteStatus;
    this.syncToParentStatus = syncToParentStatus;
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
  public BaseGitMacheteBranch getUpstreamBranch() {
    assert upstreamBranch != null : "upstreamBranch hasn't been set yet";
    return upstreamBranch;
  }

  void setUpstreamBranch(BaseGitMacheteBranch givenUpstreamBranch) {
    assert upstreamBranch == null : "upstreamBranch has already been set";
    upstreamBranch = givenUpstreamBranch;
  }

  @Override
  public List<BaseGitMacheteNonRootBranch> getDownstreamBranches() {
    return List.narrow(downstreamBranches);
  }

  @Override
  public List<IGitMacheteCommit> getCommits() {
    return List.narrow(commits);
  }

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }

  @Override
  public Option<IGitMacheteCommit> getForkPoint() {
    return Option.of(forkPoint);
  }

  @Override
  public IGitRebaseParameters getParametersForRebaseOntoParent() throws GitMacheteMissingForkPointException {
    LOG.debug(() -> "Entering: branch = '${getName()}'");
    if (forkPoint == null) {
      throw new GitMacheteMissingForkPointException("Cannot get fork point for branch '${getName()}'");
    }
    var newBaseBranch = getUpstreamBranch();

    LOG.debug(() -> "Inferred rebase parameters: currentBranch = ${getName()}, " +
        "newBaseCommit = ${newBaseBranch.getPointedCommit().getHash()}, " +
        "forkPointCommit = ${forkPoint != null ? forkPoint.getHash() : null}");

    return new GitRebaseParameters(/* currentBranch */ this, newBaseBranch.getPointedCommit(), forkPoint);
  }

  @Override
  public IGitMergeParameters getParametersForMergeIntoParent() {
    LOG.debug(() -> "Entering: branch = '${getName()}'");
    LOG.debug(() -> "Inferred merge parameters: currentBranch = ${getName()}, " +
        "branchToMergeInto = ${getUpstreamBranch().getName()}");

    return new GitMergeParameters(/* currentBranch */ this, /* branchToMergeInto */ getUpstreamBranch());
  }
}
