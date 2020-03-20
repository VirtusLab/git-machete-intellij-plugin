package com.virtuslab.gitmachete.frontend.graph.repositorygraph;

import io.vavr.collection.List;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;

public interface IBranchGetCommitsStrategy {
  List<IGitMacheteCommit> getCommitsOf(IGitMacheteBranch branch);
}
