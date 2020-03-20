package com.virtuslab.gitmachete.backend.impl;

import java.util.Optional;

import lombok.Data;

import io.vavr.collection.List;
import io.vavr.control.Try;

import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

@Data
public class GitMacheteBranch implements IGitMacheteBranch {
  private final IGitCoreLocalBranch coreLocalBranch;
  private final String name;
  @Nullable
  private final String customAnnotation;
  private final List<IGitMacheteBranch> downstreamBranches;
  private final IGitMacheteCommit pointedCommit;
  private final List<IGitMacheteCommit> commits;
  private final SyncToOriginStatus syncToOriginStatus;
  private final SyncToParentStatus syncToParentStatus;

  @Override
  public Optional<String> getCustomAnnotation() {
    return Optional.ofNullable(customAnnotation);
  }

  @Override
  public Optional<IGitMacheteCommit> computeForkPoint() throws GitMacheteException {
    return Try.of(() -> coreLocalBranch.computeForkPoint())
        .getOrElseThrow(e -> new GitMacheteException(e))
        .map(GitMacheteCommit::new);
  }
}
