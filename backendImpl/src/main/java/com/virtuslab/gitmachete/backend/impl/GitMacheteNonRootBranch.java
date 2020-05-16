package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.GitMacheteMissingForkPointException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

@Getter
@ToString(callSuper = true)
public final class GitMacheteNonRootBranch extends BaseGitMacheteBranch implements IGitMacheteNonRootBranch {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("backend");

  @ToString.Exclude
  @MonotonicNonNull
  private IGitMacheteBranch upstreamBranch = null;
  private final @Nullable IGitMacheteCommit forkPoint;
  private final List<IGitMacheteCommit> commits;
  private final SyncToParentStatus syncToParentStatus;

  public GitMacheteNonRootBranch(
      String name,
      List<GitMacheteNonRootBranch> downstreamBranches,
      IGitMacheteCommit pointedCommit,
      @Nullable IGitMacheteRemoteBranch remoteBranch,
      SyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation,
      @Nullable String statusHookOutput,
      @Nullable IGitMacheteCommit forkPoint,
      List<IGitMacheteCommit> commits,
      SyncToParentStatus syncToParentStatus) {
    super(name, downstreamBranches, pointedCommit, remoteBranch, syncToRemoteStatus, customAnnotation, statusHookOutput);

    this.forkPoint = forkPoint;
    this.commits = commits;
    this.syncToParentStatus = syncToParentStatus;

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

  @Override
  public IGitMacheteBranch getUpstreamBranch() {
    assert upstreamBranch != null : "upstreamBranch hasn't been set yet";
    return upstreamBranch;
  }

  void setUpstreamBranch(IGitMacheteBranch givenUpstreamBranch) {
    assert upstreamBranch == null : "upstreamBranch has already been set";
    upstreamBranch = givenUpstreamBranch;
  }

  @Override
  public List<IGitMacheteCommit> getCommits() {
    return List.narrow(commits);
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
