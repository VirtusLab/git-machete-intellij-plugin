package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteBranchReference;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

@Getter
@ToString
@UsesObjectEquals
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseManagedBranchSnapshot implements IManagedBranchSnapshot {
  private final String name;
  private final String fullName;
  private final List<NonRootManagedBranchSnapshot> children;
  private final ICommitOfManagedBranch pointedCommit;
  private final @Nullable IRemoteBranchReference remoteTrackingBranch;
  private final SyncToRemoteStatus syncToRemoteStatus;
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
    for (var child : children) {
      child.setParent(this);
    }
  }

  @Override
  public Option<IRemoteBranchReference> getRemoteTrackingBranch() {
    return Option.of(remoteTrackingBranch);
  }

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }

  @Override
  public Option<String> getStatusHookOutput() {
    return Option.of(statusHookOutput);
  }
}
