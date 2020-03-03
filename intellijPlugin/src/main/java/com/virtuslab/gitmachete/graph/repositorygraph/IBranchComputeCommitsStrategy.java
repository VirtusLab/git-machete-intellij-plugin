package com.virtuslab.gitmachete.graph.repositorygraph;

import java.util.List;

import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;

public interface IBranchComputeCommitsStrategy {
  List<IGitMacheteCommit> computeCommitsOf(IGitMacheteBranch branch) throws GitMacheteException;
}
