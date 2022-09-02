package com.virtuslab.gitmachete.backend.api;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import io.vavr.control.Option;

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
  public Option<IManagedBranchSnapshot> getCurrentBranchIfManaged() {
    return Option.none();
  }

  @Override
  public List<IManagedBranchSnapshot> getManagedBranches() {
    return List.empty();
  }

  @Override
  public Option<IManagedBranchSnapshot> getManagedBranchByName(String branchName) {
    return Option.none();
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
  public Option<IExecutionResult> executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters) {
    return Option.none();
  }

  @Override
  public IOngoingRepositoryOperation getOngoingRepositoryOperationInfo() {
    return new IOngoingRepositoryOperation() {
      @Override
      public OngoingRepositoryOperationType getOperationType() {
        return OngoingRepositoryOperationType.NO_OPERATION;
      }

      @Override
      public Option<String> getBaseBranchName() {
        return Option.none();
      }
    };
  }
}
