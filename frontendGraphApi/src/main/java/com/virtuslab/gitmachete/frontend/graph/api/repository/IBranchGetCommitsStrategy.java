package com.virtuslab.gitmachete.frontend.graph.api.repository;

import io.vavr.collection.List;

import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteNonRootBranch;

public interface IBranchGetCommitsStrategy {
  List<IGitMacheteCommit> getCommitsOf(IGitMacheteNonRootBranch branch);
}
