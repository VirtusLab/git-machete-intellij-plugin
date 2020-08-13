package com.virtuslab.gitmachete.backend.impl;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRemoteBranchReference;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RemoteBranchReference implements IRemoteBranchReference {
  private final String name;
  private final String fullName;
  private final String fullNameAsLocalBranchOnRemote;
  private final String remoteName;

  static RemoteBranchReference of(IGitCoreRemoteBranchSnapshot coreRemoteBranch) {
    return new RemoteBranchReference(
        coreRemoteBranch.getName(),
        coreRemoteBranch.getFullName(),
        coreRemoteBranch.getFullNameAsLocalBranchOnRemote(),
        coreRemoteBranch.getRemoteName());
  }
}
