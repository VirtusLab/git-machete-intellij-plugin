package com.virtuslab.gitmachete.backend.impl;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteSubmoduleEntry;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {
  @Nullable
  private final String repositoryName;

  @Getter
  private final List<BaseGitMacheteRootBranch> rootBranches;
  @Getter
  private final List<IGitMacheteSubmoduleEntry> submodules;
  @Getter
  private final IBranchLayout branchLayout;

  @Nullable
  private final BaseGitMacheteBranch currentBranch;

  private final Map<String, BaseGitMacheteBranch> branchByName;

  @Override
  public Optional<String> getRepositoryName() {
    return Optional.ofNullable(repositoryName);
  }

  @Override
  public Optional<BaseGitMacheteBranch> getCurrentBranchIfManaged() {
    return Optional.ofNullable(currentBranch);
  }

  @Override
  public Optional<BaseGitMacheteBranch> getBranchByName(String branchName) {
    return Optional.ofNullable(branchByName.get(branchName));
  }

  @Override
  public IGitRebaseParameters deriveParametersForRebaseOntoParent(BaseGitMacheteNonRootBranch branch)
      throws GitMacheteException {
    var newBaseBranch = branch.getUpstreamBranch();

    var forkPoint = branch.deriveForkPoint();
    if (!forkPoint.isPresent()) {
      throw new GitMacheteException(
          MessageFormat.format("Cannot find fork point for branch \"{0}\"", branch.getName()));
    }

    return new GitRebaseParameters(/* currentBranch */ branch, newBaseBranch.getPointedCommit(), forkPoint.get());
  }

  @Override
  public IGitMergeParameters deriveParametersForMergeIntoParent(BaseGitMacheteNonRootBranch branch) {
    return new GitMergeParameters(/* currentBranch */ branch, /* branchToMergeInto */ branch.getUpstreamBranch());
  }
}
