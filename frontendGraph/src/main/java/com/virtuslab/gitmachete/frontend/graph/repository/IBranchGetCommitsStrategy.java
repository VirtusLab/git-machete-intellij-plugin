package com.virtuslab.gitmachete.frontend.graph.repository;

import io.vavr.collection.List;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;

public interface IBranchGetCommitsStrategy {
  List<IGitMacheteCommit> getCommitsOf(BaseGitMacheteBranch branch);
}
