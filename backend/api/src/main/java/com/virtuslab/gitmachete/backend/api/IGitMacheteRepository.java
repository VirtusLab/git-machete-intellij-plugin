package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public interface IGitMacheteRepository {
  @UIThreadUnsafe
  IGitMacheteRepositorySnapshot createSnapshotForLayout(BranchLayout branchLayout) throws GitMacheteException;

  @UIThreadUnsafe
  @Nullable
  ILocalBranchReference inferParentForLocalBranch(
      Set<String> eligibleLocalBranchNames,
      String localBranchName) throws GitMacheteException;

  @UIThreadUnsafe
  IGitMacheteRepositorySnapshot discoverLayoutAndCreateSnapshot() throws GitMacheteException;
}
