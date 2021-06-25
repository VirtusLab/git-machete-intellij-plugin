package com.virtuslab.gitmachete.frontend.actions.common;

import lombok.Value;

@Value
public class FastForwardMergeProps {
  String movingBranchName;
  String movingBranchFullName;
  String stayingBranchName;
  String stayingBranchFullName;
}
