package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.GitMacheteMissingForkPointException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteForkPointCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

@CustomLog
@Getter
@ToString
public final class GitMacheteNonRootBranch extends BaseGitMacheteBranch implements IGitMacheteNonRootBranch {

  private @MonotonicNonNull IGitMacheteBranch parentBranch = null;
  private final @Nullable IGitMacheteForkPointCommit forkPoint;
  private final List<IGitMacheteCommit> commits;
  private final SyncToParentStatus syncToParentStatus;

  @ToString.Include(name = "parentBranch") // avoid recursive `toString` call on parent branch to avoid stack overflow
  private @Nullable String getParentBranchName() {
    return parentBranch != null ? parentBranch.getName() : null;
  }

  public GitMacheteNonRootBranch(
      String name,
      List<GitMacheteNonRootBranch> childBranches,
      IGitMacheteCommit pointedCommit,
      @Nullable IGitMacheteRemoteBranch remoteTrackingBranch,
      SyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation,
      @Nullable String statusHookOutput,
      @Nullable IGitMacheteForkPointCommit forkPoint,
      List<IGitMacheteCommit> commits,
      SyncToParentStatus syncToParentStatus) {
    super(name, childBranches, pointedCommit, remoteTrackingBranch, syncToRemoteStatus, customAnnotation, statusHookOutput);

    this.forkPoint = forkPoint;
    this.commits = commits;
    this.syncToParentStatus = syncToParentStatus;

    LOG.debug("Creating ${this}");

    // Note: since the class is final, `this` is already @Initialized at this point.
    setParentForChildBranches();
  }

  @Override
  public IGitMacheteBranch getParentBranch() {
    assert parentBranch != null : "parentBranch hasn't been set yet";
    return parentBranch;
  }

  void setParentBranch(IGitMacheteBranch givenParentBranch) {
    assert parentBranch == null : "parentBranch has already been set";
    parentBranch = givenParentBranch;
  }

  @Override
  public Option<IGitMacheteForkPointCommit> getForkPoint() {
    return Option.of(forkPoint);
  }

  @Override
  public IGitRebaseParameters getParametersForRebaseOntoParent() throws GitMacheteMissingForkPointException {
    LOG.debug(() -> "Entering: branch = '${getName()}'");
    if (forkPoint == null) {
      throw new GitMacheteMissingForkPointException("Cannot get fork point for branch '${getName()}'");
    }
    var newBaseBranch = getParentBranch();

    LOG.debug(() -> "Inferred rebase parameters: currentBranch = ${getName()}, " +
        "newBaseCommit = ${newBaseBranch.getPointedCommit().getHash()}, " +
        "forkPointCommit = ${forkPoint != null ? forkPoint.getHash() : null}");

    return new GitRebaseParameters(/* currentBranch */ this, newBaseBranch.getPointedCommit(), forkPoint);
  }

  @Override
  public IGitMergeParameters getParametersForMergeIntoParent() {
    LOG.debug(() -> "Entering: branch = '${getName()}'");
    LOG.debug(() -> "Inferred merge parameters: currentBranch = ${getName()}, " +
        "branchToMergeInto = ${getParentBranch().getName()}");

    return new GitMergeParameters(/* currentBranch */ this, /* branchToMergeInto */ getParentBranch());
  }
}
