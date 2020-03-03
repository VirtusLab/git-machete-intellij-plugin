package com.virtuslab.gitcore.impl.jgit;

import java.text.MessageFormat;

import lombok.Data;

import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;

@Data(staticConstructor = "of")
public class GitCoreBranchTrackingStatus implements IGitCoreBranchTrackingStatus {
  private final int ahead;
  private final int behind;

  @Override
  public String toString() {
    return MessageFormat.format("Ahead: {0}; Behind: {1}", ahead, behind);
  }
}
