package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteMissingForkPointException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {

  @Getter
  private final List<BaseGitMacheteRootBranch> rootBranches;
  @Getter
  private final IBranchLayout branchLayout;

  @Nullable
  private final BaseGitMacheteBranch currentBranch;

  private final Map<String, BaseGitMacheteBranch> branchByName;

  @Override
  public Option<BaseGitMacheteBranch> getCurrentBranchIfManaged() {
    return Option.of(currentBranch);
  }

  @Override
  public Option<BaseGitMacheteBranch> getBranchByName(String branchName) {
    return branchByName.get(branchName);
  }

  @Override
  public IGitRebaseParameters getParametersForRebaseOntoParent(BaseGitMacheteNonRootBranch branch)
      throws GitMacheteMissingForkPointException {
    var forkPoint = branch.getForkPoint();
    if (forkPoint.isEmpty()) {
      throw new GitMacheteMissingForkPointException("Cannot get fork point for branch '${branch.getName()}'");
    }

    var newBaseBranch = branch.getUpstreamBranch();
    return new GitRebaseParameters(/* currentBranch */ branch, newBaseBranch.getPointedCommit(), forkPoint.get());
  }

  @Override
  public IGitMergeParameters deriveParametersForMergeIntoParent(BaseGitMacheteNonRootBranch branch) {
    return new GitMergeParameters(/* currentBranch */ branch, /* branchToMergeInto */ branch.getUpstreamBranch());
  }
}
