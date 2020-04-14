package com.virtuslab.gitcore.impl.jgit;

import lombok.Data;

import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;

@Data(staticConstructor = "of")
public class GitCoreBranchTrackingStatus implements IGitCoreBranchTrackingStatus {
  private final int ahead;
  private final int behind;
  private final String remoteName;

  @Override
  public String toString() {
    return "Remote: ${remoteName}; Ahead: ${ahead}; Behind: ${behind}";
  }
}
