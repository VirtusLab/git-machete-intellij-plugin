package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.IGitCoreBranchTrackingStatus;
import java.text.MessageFormat;
import lombok.Data;

@Data(staticConstructor = "of")
public class JGitBranchTrackingStatus implements IGitCoreBranchTrackingStatus {
  private final int ahead;
  private final int behind;

  @Override
  public String toString() {
    return MessageFormat.format("Ahead: {0}; Behind: {1}", ahead, behind);
  }
}
