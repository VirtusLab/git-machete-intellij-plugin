package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import lombok.CustomLog;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.ICommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IForkPointCommitOfManagedBranch;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;
import com.virtuslab.gitmachete.backend.api.IRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;

@CustomLog
@ToString
public final class RootManagedBranchSnapshot extends BaseManagedBranchSnapshot implements IRootManagedBranchSnapshot {

  public RootManagedBranchSnapshot(
      String name,
      String fullName,
      List<NonRootManagedBranchSnapshot> children,
      ICommitOfManagedBranch pointedCommit,
      @Nullable IRemoteTrackingBranchReference remoteTrackingBranch,
      RelationToRemote relationToRemote,
      @Nullable String customAnnotation,
      @Nullable String statusHookOutput,
      @Nullable IForkPointCommitOfManagedBranch remoteForkPoint) {
    super(name, fullName, children, pointedCommit, remoteTrackingBranch, relationToRemote, remoteForkPoint, customAnnotation,
        statusHookOutput);

    LOG.debug("Creating ${this}");

    // Note: since the class is final, `this` is already @Initialized at this point.
    setParentForChildren();
  }
}
