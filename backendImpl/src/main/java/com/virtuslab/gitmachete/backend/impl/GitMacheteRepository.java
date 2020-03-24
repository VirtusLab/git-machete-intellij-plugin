package com.virtuslab.gitmachete.backend.impl;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import io.vavr.collection.List;
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

  private final Map<String, Optional<IGitMacheteBranch>> branchNameToUpstream = new TreeMap<>();

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
  public Optional<IGitMacheteBranch> deriveExistingUpstreamBranch(IGitMacheteBranch branch) {
    Predicate<IGitMacheteBranch> isUpstreamForBranch = gitMacheteBranch -> gitMacheteBranch.getDownstreamBranches()
        .contains(branch);
    return branchNameToUpstream.computeIfAbsent(branch.getName(),
        branchName -> findBranchRecursively(rootBranches, isUpstreamForBranch));
  }

  @Override
  public IGitRebaseParameters deriveParametersForRebaseOntoParent(IGitMacheteBranch branch) throws GitMacheteException {
    IGitMacheteBranch newBaseBranch = deriveUpstreamBranch(branch);

    var forkPoint = branch.deriveForkPoint();
    if (!forkPoint.isPresent()) {
      throw new GitMacheteException(
          MessageFormat.format("Cannot find fork point for branch \"{0}\"", branch.getName()));
    }

    return new GitRebaseParameters(/* currentBranch */ branch, newBaseBranch.getPointedCommit(), forkPoint.get());
  }

  @Override
  public IGitMergeParameters deriveParametersForMergeIntoParent(IGitMacheteBranch branch) throws GitMacheteException {
    IGitMacheteBranch branchToMergeInto = deriveUpstreamBranch(branch);
    return new GitMergeParameters(/* currentBranch */ branch, branchToMergeInto);
  }

  private IGitMacheteBranch deriveUpstreamBranch(IGitMacheteBranch branch) throws GitMacheteException {
    if (rootBranches.contains(branch)) {
      throw new GitMacheteException(
          MessageFormat.format("Cannot get merge parameters for root branch \"{0}\"", branch.getName()));
    }

    Optional<IGitMacheteBranch> newBaseBranch = deriveExistingUpstreamBranch(branch);
    if (!newBaseBranch.isPresent()) {
      throw new GitMacheteException(
          MessageFormat.format("git-machete does not manage branch: \"{0}\"", branch.getName()));
    }
    return newBaseBranch.get();
  }

  /** @return the first branch in pre-order traversal that satisfies the {@code predicate} */
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
