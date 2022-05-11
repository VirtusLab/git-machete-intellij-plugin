package com.virtuslab.gitmachete.backend.impl;

import lombok.Getter;
import lombok.ToString;

import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;

@Getter
@ToString
public final class LocalBranchReference extends BaseBranchReference implements ILocalBranchReference {
  private LocalBranchReference(String name, String fullName) {
    super(name, fullName);
  }

  static LocalBranchReference of(IGitCoreLocalBranchSnapshot coreLocalBranch) {
    return new LocalBranchReference(
        coreLocalBranch.getName(),
        coreLocalBranch.getFullName());
  }
}
