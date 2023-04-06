package com.virtuslab.gitmachete.backend.impl;

import java.nio.file.Path;

import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRootManagedBranchSnapshot;

@RequiredArgsConstructor
public class GitMacheteRepositorySnapshot implements IGitMacheteRepositorySnapshot {

  @Getter
  private final Path mainGitDirectoryPath;

  @Getter
  private final List<IRootManagedBranchSnapshot> rootBranches;

  private final BranchLayout branchLayout;

  private final @Nullable IManagedBranchSnapshot currentBranchIfManaged;

  // Using LinkedHashMap to retain the original order of branches.
  private final LinkedHashMap<String, IManagedBranchSnapshot> managedBranchByName;

  @Getter
  private final Set<String> duplicatedBranchNames;

  @Getter
  private final Set<String> skippedBranchNames;

  @Getter
  private final OngoingRepositoryOperation ongoingRepositoryOperation;

  @Override
  public BranchLayout getBranchLayout() {
    return branchLayout;
  }

  @Override
  public @Nullable IManagedBranchSnapshot getCurrentBranchIfManaged() {
    return currentBranchIfManaged;
  }

  @Override
  public List<IManagedBranchSnapshot> getManagedBranches() {
    return managedBranchByName.values().toList();
  }

  @Override
  public @Nullable IManagedBranchSnapshot getManagedBranchByName(String branchName) {
    return managedBranchByName.get(branchName).getOrNull();
  }
}
