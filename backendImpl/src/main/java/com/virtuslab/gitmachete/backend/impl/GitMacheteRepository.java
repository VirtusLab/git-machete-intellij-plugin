package com.virtuslab.gitmachete.backend.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteSubmoduleEntry;

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
  public Optional<@Nullable String> getRepositoryName() {
    return Optional.ofNullable(repositoryName);
  }

  @Override
  public Optional<@Nullable IGitMacheteBranch> getCurrentBranchIfManaged() {
    return Optional.ofNullable(currentBranch);
  }

  @Override
  public Optional<@Nullable IGitMacheteBranch> getBranchByName(String branchName) {
    return Optional.ofNullable(branchByName.get(branchName));
  }
}
