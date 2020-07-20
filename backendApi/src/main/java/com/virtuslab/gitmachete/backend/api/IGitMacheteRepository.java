package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.Set;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IGitMacheteRepository {
  IGitMacheteRepositorySnapshot createSnapshotForLayout(IBranchLayout branchLayout) throws GitMacheteException;

  Option<String> inferParentForLocalBranch(
      Set<String> eligibleBranchNames,
      String localBranchName) throws GitMacheteException;

  IGitMacheteRepositorySnapshot discoverLayoutAndCreateSnapshot() throws GitMacheteException;
}
