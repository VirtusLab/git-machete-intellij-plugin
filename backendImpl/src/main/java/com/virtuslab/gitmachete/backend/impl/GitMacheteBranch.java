package com.virtuslab.gitmachete.backend.impl;

import java.util.Optional;

import lombok.Data;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

@Data
public class GitMacheteBranch implements IGitMacheteBranch {
  private final String name;
  @Nullable
  private final String customAnnotation;
  private final List<IGitMacheteBranch> downstreamBranches;
  private final IGitMacheteCommit pointedCommit;
  private final List<IGitMacheteCommit> commits;
  private final SyncToOriginStatus syncToOriginStatus;
  private final SyncToParentStatus syncToParentStatus;
  private final IGitCoreLocalBranch coreLocalBranch;

  @Override
  public Optional<String> getCustomAnnotation() {
    return Optional.ofNullable(customAnnotation);
  }

  @Override
  public Optional<IGitMacheteCommit> deriveForkPoint() throws GitMacheteException {
    return Try.of(() -> coreLocalBranch.deriveForkPoint())
        .getOrElseThrow(e -> new GitMacheteException(e))
        .map(GitMacheteCommit::new);
  }
}
