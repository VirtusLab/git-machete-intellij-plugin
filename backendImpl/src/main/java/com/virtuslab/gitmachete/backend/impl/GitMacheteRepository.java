package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;
import com.virtuslab.gitmachete.backend.impl.hooks.PreRebaseHookExecutor;

@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {
  @Getter(onMethod_ = {@Override})
  private final List<IGitMacheteRootBranch> rootBranches;

  private final @Nullable IBranchLayout branchLayout;

  private final @Nullable IGitMacheteBranch currentBranchIfManaged;

  private final Map<String, IGitMacheteBranch> branchByName;

  private final PreRebaseHookExecutor preRebaseHookExecutor;

  @Override
  public Option<IBranchLayout> getBranchLayout() {
    return Option.of(branchLayout);
  }

  @Override
  public Option<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Option.of(currentBranchIfManaged);
  }

  @Override
  public Option<IGitMacheteBranch> getBranchByName(String branchName) {
    return branchByName.get(branchName);
  }

  @Override
  public Option<Integer> executeMachetePreRebaseHookIfPresent(IGitRebaseParameters gitRebaseParameters)
      throws GitMacheteException {
    return preRebaseHookExecutor.executeHookFor(gitRebaseParameters);
  }
}
