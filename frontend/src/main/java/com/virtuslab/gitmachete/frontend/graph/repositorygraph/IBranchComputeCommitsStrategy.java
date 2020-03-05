package com.virtuslab.gitmachete.frontend.graph.repositorygraph;

import java.util.List;

import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;

public interface IBranchComputeCommitsStrategy {
  List<IGitMacheteCommit> computeCommitsOf(IGitMacheteBranch branch) throws GitMacheteException;
}
