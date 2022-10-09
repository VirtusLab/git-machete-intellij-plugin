package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.GitMacheteMissingForkPointException;
import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IForkPointCommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;

@Getter
@ToString
@CustomLog
@UsesObjectEquals
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseManagedBranchSnapshot implements IManagedBranchSnapshot {
  private final String name;
  private final String fullName;
  private final List<NonRootManagedBranchSnapshot> children;
  private final ICommitOfManagedBranch pointedCommit;
  private final @Nullable IRemoteTrackingBranchReference remoteTrackingBranch;
  private final RelationToRemote relationToRemote;

  private final @Nullable IForkPointCommitOfManagedBranch remoteForkPoint;
  private final @Nullable String customAnnotation;
  private final @Nullable String statusHookOutput;

  @ToString.Include(name = "children") // avoid recursive `toString` calls on child branches
  private List<String> getChildNames() {
    return children.map(e -> e.getName());
  }

  /**
   * This is a hack necessary to create an immutable cyclic structure
   * (children pointing at the parent and the parent pointing at the children).
   * This is definitely not the cleanest solution, but still easier to manage and reason about than keeping the
   * parent data somewhere outside of this class (e.g. in {@link GitMacheteRepositorySnapshot}).
   */
  protected void setParentForChildren() {
    for (val child : children) {
      child.setParent(this);
    }
  }

  @Override
  public @Nullable IRemoteTrackingBranchReference getRemoteTrackingBranch() {
    return remoteTrackingBranch;
  }

  @Override
  public @Nullable String getCustomAnnotation() {
    return customAnnotation;
  }

  @Override
  public @Nullable String getStatusHookOutput() {
    return statusHookOutput;
  }

  @Override
  public IGitRebaseParameters getParametersForRebaseOntoRemote() throws GitMacheteMissingForkPointException {

    LOG.debug(() -> "Entering: branch = '${getName()}'");
    val newBaseBranch = getRemoteTrackingBranch();
    if (newBaseBranch == null || remoteForkPoint == null) {
      throw new GitMacheteMissingForkPointException("Cannot get remote fork point for branch '${getName()}'");
    } else {

      LOG.debug(() -> "Inferred rebase parameters: currentBranch = ${getName()}, " +
          "forkPointCommit = ${remoteForkPoint != null ? remoteForkPoint.getHash() : null}");

      return new GitRebaseParameters(/* currentBranch */ this, newBaseBranch, remoteForkPoint);
    }

  }
}
