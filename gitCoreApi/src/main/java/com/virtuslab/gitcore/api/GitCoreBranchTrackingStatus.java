package com.virtuslab.gitcore.api;

import lombok.Data;

@Data(staticConstructor = "of")
public class GitCoreBranchTrackingStatus {
  private final int ahead;
  private final int behind;
  private final String remoteName;

  @Override
  public String toString() {
    return "Remote: ${remoteName}; Ahead: ${ahead}; Behind: ${behind}";
  }
}
