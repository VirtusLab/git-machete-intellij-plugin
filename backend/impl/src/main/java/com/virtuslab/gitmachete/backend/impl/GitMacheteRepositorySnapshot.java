package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.OngoingRepositoryOperationType;
import com.virtuslab.gitmachete.backend.api.hooks.IExecutionResult;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;

@RequiredArgsConstructor
public class GitMacheteRepositorySnapshot implements IGitMacheteRepositorySnapshot {

  @Getter
  private final List<IRootManagedBranchSnapshot> rootBranches;

  private final IBranchLayout branchLayout;

  private final @Nullable IManagedBranchSnapshot currentBranchIfManaged;

  private final Map<String, IManagedBranchSnapshot> managedBranchByName;

  @Getter
  private final Set<String> duplicatedBranchNames;

  @Getter
  private final Set<String> skippedBranchNames;

  private final PreRebaseHookExecutor preRebaseHookExecutor;

  @Data
  static class OngoingRepositoryOperation implements IOngoingRepositoryOperation {
    private final OngoingRepositoryOperationType operationType;

    private final Option<String> baseBranchName;
  }

  @Getter
  private final OngoingRepositoryOperation ongoingRepositoryOperationInfo;

  @Override
  public IBranchLayout getBranchLayout() {
    return branchLayout;
  }

  @Override
  public Option<IManagedBranchSnapshot> getCurrentBranchIfManaged() {
    return Option.of(currentBranchIfManaged);
  }

  @Override
  public List<IManagedBranchSnapshot> getManagedBranches() {
    return managedBranchByName.values().toList();
  }

  @Override
  public Option<IManagedBranchSnapshot> getManagedBranchByName(String branchName) {
    return managedBranchByName.get(branchName);
  }

  @Override
  public Option<IExecutionResult> executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters)
      throws GitMacheteException {
    return preRebaseHookExecutor.executeHookFor(gitRebaseParameters);
  }
}
