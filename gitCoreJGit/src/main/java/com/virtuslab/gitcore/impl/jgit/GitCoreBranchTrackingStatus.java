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
    return String.format("Remote: %s; Ahead: %d; Behind: %d", remoteName, ahead, behind);
  }
}
