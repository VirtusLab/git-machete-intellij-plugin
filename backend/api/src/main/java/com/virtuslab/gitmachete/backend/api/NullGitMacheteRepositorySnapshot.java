package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;

public final class NullGitMacheteRepositorySnapshot implements IGitMacheteRepositorySnapshot {
  private static final NullGitMacheteRepositorySnapshot instance = new NullGitMacheteRepositorySnapshot();

  private NullGitMacheteRepositorySnapshot() {}

  public static IGitMacheteRepositorySnapshot getInstance() {
    return instance;
  }

  @Override
  public IBranchLayout getBranchLayout() {
    return NullBranchLayout.getInstance();
  }

  @Override
  public List<IRootManagedBranchSnapshot> getRootBranches() {
    return List.empty();
  }

  @Override
  public @Nullable IManagedBranchSnapshot getCurrentBranchIfManaged() {
    return null;
  }

  @Override
  public List<IManagedBranchSnapshot> getManagedBranches() {
    return List.empty();
  }

  @Override
  public @Nullable IManagedBranchSnapshot getManagedBranchByName(String branchName) {
    return null;
  }

  @Override
  public Set<String> getDuplicatedBranchNames() {
    return TreeSet.empty();
  }

  @Override
  public Set<String> getSkippedBranchNames() {
    return TreeSet.empty();
  }

  @Override
  public @Nullable IExecutionResult executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters) {
    return null;
  }

  @Getter
  public final OngoingRepositoryOperation ongoingRepositoryOperation = new OngoingRepositoryOperation(
      OngoingRepositoryOperationType.NO_OPERATION, null);

}
