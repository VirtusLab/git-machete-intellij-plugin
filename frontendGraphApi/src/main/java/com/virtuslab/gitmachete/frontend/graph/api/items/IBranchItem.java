package com.virtuslab.gitmachete.frontend.graph.api.items;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public interface IBranchItem extends IGraphItem {

  BaseGitMacheteBranch getBranch();

  SyncToRemoteStatus getSyncToRemoteStatus();
}
