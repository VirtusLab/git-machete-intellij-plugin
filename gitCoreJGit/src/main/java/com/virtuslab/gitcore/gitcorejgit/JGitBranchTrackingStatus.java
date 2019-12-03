package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.IGitCoreBranchTrackingStatus;
import java.text.MessageFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JGitBranchTrackingStatus implements IGitCoreBranchTrackingStatus {
  @Getter private int ahead;
  @Getter private int behind;

  static JGitBranchTrackingStatus build(int ahead, int behind) {
    return new JGitBranchTrackingStatus(ahead, behind);
  }

  @Override
  public String toString() {
    return MessageFormat.format("Ahead: {0}; Behind: {1}", ahead, behind);
  }
}
