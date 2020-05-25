package com.virtuslab.gitcore.api;

import lombok.Data;
import lombok.ToString;

@Data(staticConstructor = "of")
@ToString
public class GitCoreBranchTrackingStatus {
  private final int ahead;
  private final int behind;
}
