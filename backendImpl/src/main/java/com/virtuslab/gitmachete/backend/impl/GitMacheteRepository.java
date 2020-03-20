package com.virtuslab.gitmachete.backend.impl;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.vavr.collection.List;

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

  private final Map<String, IGitMacheteBranch> branchByName;

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
    return Optional.ofNullable(branchByName.get(branchName));
  }

  @Override
  public Optional<IGitMacheteBranch> deriveUpstreamBranch(IGitMacheteBranch branch) {
    return findBranchRecursively(rootBranches, b -> b.getDownstreamBranches().contains(branch));
  }

  @Override
  public IGitRebaseParameters computeRebaseOntoParentParameters(IGitMacheteBranch branch) throws GitMacheteException {
    IGitMacheteBranch newBaseBranch = deriveUpstreamWithChecks(branch);

    var forkPoint = branch.computeForkPoint();
    if (forkPoint.isEmpty()) {
      throw new GitMacheteException(
          MessageFormat.format("Can not find fork point for branch \"{0}\"", branch.getName()));
    }

    return new GitRebaseParameters(/* currentBranch */ branch, newBaseBranch.getPointedCommit(), forkPoint.get());
  }

  @Override
  public IGitMergeParameters getMergeIntoParentParameters(IGitMacheteBranch branch) throws GitMacheteException {
    IGitMacheteBranch newBaseBranch = deriveUpstreamWithChecks(branch);
    return new GitMergeParameters(/* currentBranch */ branch, newBaseBranch);
  }

  private IGitMacheteBranch deriveUpstreamWithChecks(IGitMacheteBranch branch) throws GitMacheteException {
    if (rootBranches.contains(branch)) {
      throw new GitMacheteException(
          MessageFormat.format("Can not get rebase parameters for root branch \"{0}\"", branch.getName()));
    }

    Optional<IGitMacheteBranch> newBaseBranch = deriveUpstreamBranch(branch);
    if (newBaseBranch.isEmpty()) {
      throw new GitMacheteException(
          MessageFormat.format("Repository does not own branch: \"{0}\"", branch.getName()));
    }
    return newBaseBranch.get();
  }

  /** Recursively traverses the list for an element that satisfies the {@code predicate}. */
  private static Optional<IGitMacheteBranch> findBranchRecursively(
      List<IGitMacheteBranch> branches,
      Predicate<IGitMacheteBranch> predicate) {
    for (var branch : branches) {
      if (predicate.test(branch)) {
        return Optional.of(branch);
      }

      var result = findBranchRecursively(branch.getDownstreamBranches(), predicate);
      if (result.isPresent()) {
        return result;
      }
    }

    return Optional.empty();
  }
}
