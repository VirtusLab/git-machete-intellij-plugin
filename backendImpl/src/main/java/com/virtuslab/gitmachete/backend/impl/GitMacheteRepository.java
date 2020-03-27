package com.virtuslab.gitmachete.backend.impl;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteSubmoduleEntry;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {
  @Nullable
  private final String repositoryName;

  @Getter
  private final List<IGitMacheteBranch> rootBranches;
  @Getter
  private final List<IGitMacheteSubmoduleEntry> submodules;
  @Getter
  private final IBranchLayout branchLayout;

  @Nullable
  private final IGitMacheteBranch currentBranch;

  private final Map<String, IGitMacheteBranch> branchNameToBranch;

  @Override
  public Optional<String> getRepositoryName() {
    return Optional.ofNullable(repositoryName);
  }

  @Override
  public Optional<IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Optional.ofNullable(currentBranch);
  }

  @Override
  public Optional<IGitMacheteBranch> getBranchByName(String branchName) {
    return Optional.ofNullable(branchNameToBranch.get(branchName));
  }

  @Override
  public IGitRebaseParameters deriveParametersForRebaseOntoParent(IGitMacheteBranch branch) throws GitMacheteException {
    var newBaseBranch = branch.getUpstreamBranch();
    if (!newBaseBranch.isPresent()) {
      throw new GitMacheteException(
          MessageFormat.format("Branch \"{0}\" doesn't have an upstream", branch.getName()));
    }

    var forkPoint = branch.deriveForkPoint();
    if (!forkPoint.isPresent()) {
      throw new GitMacheteException(
          MessageFormat.format("Cannot find fork point for branch \"{0}\"", branch.getName()));
    }

    return new GitRebaseParameters(/* currentBranch */ branch, newBaseBranch.get().getPointedCommit(), forkPoint.get());
  }

  @Override
  public IGitMergeParameters deriveParametersForMergeIntoParent(IGitMacheteBranch branch) throws GitMacheteException {
    var branchToMergeInto = branch.getUpstreamBranch();
    if (!branchToMergeInto.isPresent()) {
      throw new GitMacheteException(
          MessageFormat.format("Branch \"{0}\" doesn't have an upstream", branch.getName()));
    }

    return new GitMergeParameters(/* currentBranch */ branch, branchToMergeInto.get());
  }
}
