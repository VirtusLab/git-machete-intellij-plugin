package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

@Getter
@ToString
@UsesObjectEquals
public abstract class BaseGitMacheteBranch implements IGitMacheteBranch {
  private final String name;
  private final List<GitMacheteNonRootBranch> childBranches;
  private final IGitMacheteCommit pointedCommit;
  private final SyncToRemoteStatus syncToRemoteStatus;
  private final @Nullable IGitMacheteRemoteBranch remoteTrackingBranch;
  private final @Nullable String customAnnotation;
  private final @Nullable String statusHookOutput;

  @ToString.Include(name = "childBranches") // avoid recursive `toString` calls on child branches
  private List<String> getChildBranchNames() {
    return childBranches.map(e -> e.getName());
  }

  protected BaseGitMacheteBranch(
      String name,
      List<GitMacheteNonRootBranch> childBranches,
      IGitMacheteCommit pointedCommit,
      @Nullable IGitMacheteRemoteBranch remoteTrackingBranch,
      SyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation,
      @Nullable String statusHookOutput) {
    this.name = name;
    this.childBranches = childBranches;
    this.pointedCommit = pointedCommit;
    this.syncToRemoteStatus = syncToRemoteStatus;
    this.remoteTrackingBranch = remoteTrackingBranch;
    this.customAnnotation = customAnnotation;
    this.statusHookOutput = statusHookOutput;
  }

  /**
   * This is a hack necessary to create an immutable cyclic structure
   * (children pointing at the parent and the parent pointing at the children).
   * This is definitely not the cleanest solution, but still easier to manage and reason about than keeping the
   * parent data somewhere outside of this class (e.g. in {@link GitMacheteRepositorySnapshot}).
   */
  protected void setParentForChildBranches() {
    for (GitMacheteNonRootBranch branch : childBranches) {
      branch.setParentBranch(this);
    }
  }

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }

  @Override
  public Option<String> getStatusHookOutput() {
    return Option.of(statusHookOutput);
  }

  @Override
  public Option<IGitMacheteRemoteBranch> getRemoteTrackingBranch() {
    return Option.of(remoteTrackingBranch);
  }
}
