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
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("backend");

  @Getter
  private final List<IGitMacheteRootBranch> rootBranches;

  @Nullable
  private final IBranchLayout branchLayout;

  @Override
  public Option<IBranchLayout> getBranchLayout() {
    return Option.of(branchLayout);
  }

  @Nullable
  private final IGitMacheteBranch currentBranch;

  private final Map<String, IGitMacheteBranch> branchByName;

  @Override
  public Option<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Option.of(currentBranch);
  }

  @Override
  public Option<IGitMacheteBranch> getBranchByName(String branchName) {
    return branchByName.get(branchName);
  }
}
