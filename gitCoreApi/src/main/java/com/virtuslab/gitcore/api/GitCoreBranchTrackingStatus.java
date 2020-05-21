package com.virtuslab.gitcore.api;

import lombok.Data;

@Data(staticConstructor = "of")
public class GitCoreBranchTrackingStatus {
  private final String remoteName;
  private final int ahead;
  private final int behind;

  @Override
  public String toString() {
    return "Remote: ${remoteName}; Ahead: ${ahead}; Behind: ${behind}";
  }
}
