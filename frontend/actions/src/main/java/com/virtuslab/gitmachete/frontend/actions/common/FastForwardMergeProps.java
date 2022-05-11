package com.virtuslab.gitmachete.frontend.actions.common;

import lombok.Value;

import com.virtuslab.gitmachete.backend.api.IBranchReference;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;

@Value
public class FastForwardMergeProps {
  ILocalBranchReference movingBranch;
  IBranchReference stayingBranch;
}
