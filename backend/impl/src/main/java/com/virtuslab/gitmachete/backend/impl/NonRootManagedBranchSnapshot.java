package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.GitMacheteMissingForkPointException;
import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IForkPointCommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

@CustomLog
@Getter
@ToString
public final class NonRootManagedBranchSnapshot extends BaseManagedBranchSnapshot implements INonRootManagedBranchSnapshot {

  private @MonotonicNonNull IManagedBranchSnapshot parent = null;
  private final @Nullable IForkPointCommitOfManagedBranch forkPoint;
  private final List<ICommitOfManagedBranch> uniqueCommits;

  private final List<ICommitOfManagedBranch> commitsUntilParent;
  private final SyncToParentStatus syncToParentStatus;

  @ToString.Include(name = "parent") // avoid recursive `toString` call on parent branch to avoid stack overflow
  private @Nullable String getParentName() {
    return parent != null ? parent.getName() : null;
  }

  public NonRootManagedBranchSnapshot(
      String name,
      String fullName,
      List<NonRootManagedBranchSnapshot> children,
      ICommitOfManagedBranch pointedCommit,
      @Nullable IRemoteTrackingBranchReference remoteTrackingBranch,
      RelationToRemote relationToRemote,
      @Nullable String customAnnotation,
      @Nullable String statusHookOutput,
      @Nullable IForkPointCommitOfManagedBranch forkPoint,
      List<ICommitOfManagedBranch> uniqueCommits,
      List<ICommitOfManagedBranch> commitsUntilParent,
      SyncToParentStatus syncToParentStatus) {
    super(name, fullName, children, pointedCommit, remoteTrackingBranch, relationToRemote, customAnnotation, statusHookOutput);

    this.forkPoint = forkPoint;
    this.uniqueCommits = uniqueCommits;
    this.commitsUntilParent = commitsUntilParent;
    this.syncToParentStatus = syncToParentStatus;

    LOG.debug("Creating ${this}");

    // Note: since the class is final, `this` is already @Initialized at this point.
    setParentForChildren();
  }

  @Override
  public IManagedBranchSnapshot getParent() {
    assert parent != null : "parentBranch hasn't been set yet";
    return parent;
  }

  void setParent(IManagedBranchSnapshot givenParentBranch) {
    assert parent == null : "parentBranch has already been set";
    parent = givenParentBranch;
  }

  @Override
  public @Nullable IForkPointCommitOfManagedBranch getForkPoint() {
    return forkPoint;
  }

  @Override
  public IGitRebaseParameters getParametersForRebaseOntoParent() throws GitMacheteMissingForkPointException {
    LOG.debug(() -> "Entering: branch = '${getName()}'");
    if (forkPoint == null) {
      throw new GitMacheteMissingForkPointException("Cannot get fork point for branch '${getName()}'");
    }
    val newBaseBranch = getParent();

    LOG.debug(() -> "Inferred rebase parameters: currentBranch = ${getName()}, " +
        "newBaseCommit = ${newBaseBranch.getPointedCommit().getHash()}, " +
        "forkPointCommit = ${forkPoint != null ? forkPoint.getHash() : null}");

    return new GitRebaseParameters(/* currentBranch */ this, newBaseBranch, forkPoint);
  }
}
