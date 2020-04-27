package com.virtuslab.gitmachete.frontend.graph.api.repository;

import io.vavr.collection.List;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;

public interface IBranchGetCommitsStrategy {
  List<IGitMacheteCommit> getCommitsOf(BaseGitMacheteNonRootBranch branch);
}
