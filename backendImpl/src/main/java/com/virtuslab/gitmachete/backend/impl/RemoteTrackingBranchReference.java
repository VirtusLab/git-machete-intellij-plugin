package com.virtuslab.gitmachete.backend.impl;

import lombok.Getter;
import lombok.ToString;

import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;

@Getter
@ToString
public final class RemoteTrackingBranchReference extends BaseBranchReference implements IRemoteTrackingBranchReference {
  private final String fullNameAsLocalBranchOnRemote;
  private final String remoteName;
  private final ILocalBranchReference trackedLocalBranch;

  private RemoteTrackingBranchReference(
      String name, String fullName, String fullNameAsLocalBranchOnRemote, String remoteName,
      ILocalBranchReference trackedLocalBranch) {
    super(name, fullName);
    this.fullNameAsLocalBranchOnRemote = fullNameAsLocalBranchOnRemote;
    this.remoteName = remoteName;
    this.trackedLocalBranch = trackedLocalBranch;
  }

  static RemoteTrackingBranchReference of(IGitCoreRemoteBranchSnapshot coreRemoteBranch,
      IGitCoreLocalBranchSnapshot coreTrackedLocalBranch) {
    return new RemoteTrackingBranchReference(
        coreRemoteBranch.getName(),
        coreRemoteBranch.getFullName(),
        coreRemoteBranch.getFullNameAsLocalBranchOnRemote(),
        coreRemoteBranch.getRemoteName(),
        LocalBranchReference.of(coreTrackedLocalBranch));
  }
}
