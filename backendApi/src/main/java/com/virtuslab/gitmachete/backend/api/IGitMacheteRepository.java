package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.Set;
import io.vavr.control.Option;

import com.virtuslab.branchlayout.api.IBranchLayout;

public interface IGitMacheteRepository {
  IGitMacheteRepositorySnapshot createSnapshotForLayout(IBranchLayout branchLayout) throws GitMacheteException;

  Option<ILocalBranchReference> inferParentForLocalBranch(
      Set<String> eligibleLocalBranchNames,
      String localBranchName) throws GitMacheteException;

  IGitMacheteRepositorySnapshot discoverLayoutAndCreateSnapshot() throws GitMacheteException;
}
