package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRootBranch;

@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {
  @Getter(onMethod_ = {@Override})
  private final List<IGitMacheteRootBranch> rootBranches;

  private final @Nullable IBranchLayout branchLayout;

  private final @Nullable IGitMacheteBranch currentBranch;

  private final Map<String, IGitMacheteBranch> branchByName;

  @Override
  public Option<IBranchLayout> getBranchLayout() {
    return Option.of(branchLayout);
  }

  @Override
  public Option<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Option.of(currentBranch);
  }

  @Override
  public Option<IGitMacheteBranch> getBranchByName(String branchName) {
    return branchByName.get(branchName);
  }
}
