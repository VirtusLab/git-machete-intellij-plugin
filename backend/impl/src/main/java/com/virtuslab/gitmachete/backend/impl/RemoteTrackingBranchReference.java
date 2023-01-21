package com.virtuslab.gitmachete.backend.impl;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;

import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IRemoteTrackingBranchReference;

@ExtensionMethod(LocalBranchReference.class)
@Getter
@ToString
public final class RemoteTrackingBranchReference extends BaseBranchReference implements IRemoteTrackingBranchReference {
  private final String remoteName;
  private final ILocalBranchReference trackedLocalBranch;

  private RemoteTrackingBranchReference(
      String name, String fullName, String remoteName,
      ILocalBranchReference trackedLocalBranch) {
    super(name, fullName);
    this.remoteName = remoteName;
    this.trackedLocalBranch = trackedLocalBranch;
  }

  public static RemoteTrackingBranchReference of(IGitCoreRemoteBranchSnapshot coreRemoteBranch,
      IGitCoreLocalBranchSnapshot coreTrackedLocalBranch) {
    return new RemoteTrackingBranchReference(
        coreRemoteBranch.getName(),
        coreRemoteBranch.getFullName(),
        coreRemoteBranch.getRemoteName(),
        coreTrackedLocalBranch.toLocalBranchReference());
  }
}
