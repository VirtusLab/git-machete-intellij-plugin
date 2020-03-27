package com.virtuslab.gitmachete.backend.impl;

import java.util.Optional;

import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

@Getter
@EqualsAndHashCode
@ToString
public class GitMacheteBranch implements IGitMacheteBranch {
  private final String name;
  @MonotonicNonNull
  private GitMacheteBranch upstreamBranch = null;
  private final List<GitMacheteBranch> downstreamBranches;
  private final GitMacheteCommit pointedCommit;
  private final List<GitMacheteCommit> commits;
  private final SyncToOriginStatus syncToOriginStatus;
  private final SyncToParentStatus syncToParentStatus;
  private final IGitCoreLocalBranch coreLocalBranch;
  @Nullable
  private final String customAnnotation;

  @SuppressWarnings("argument.type.incompatible")
  public GitMacheteBranch(final String name, final List<GitMacheteBranch> downstreamBranches,
      final GitMacheteCommit pointedCommit, final List<GitMacheteCommit> commits,
      final SyncToOriginStatus syncToOriginStatus, final SyncToParentStatus syncToParentStatus,
      final IGitCoreLocalBranch coreLocalBranch, @Nullable final String customAnnotation) {
    this.name = name;
    this.downstreamBranches = downstreamBranches;
    this.pointedCommit = pointedCommit;
    this.commits = commits;
    this.syncToOriginStatus = syncToOriginStatus;
    this.syncToParentStatus = syncToParentStatus;
    this.coreLocalBranch = coreLocalBranch;
    this.customAnnotation = customAnnotation;

    // This is a hack necessary to create an immutable cyclic structure (children pointing at the parent & parent
    // pointing at the children).
    // This is definitely not the cleanest solution, but still easier to manage and reason about than keeping the
    // upstream data somewhere outside of GitMacheteBranch (e.g. in GitMacheteRepository).
    for (GitMacheteBranch branch : downstreamBranches) {
      branch.setUpstreamBranch(this);
    }
  }

  @Override
  public Optional<IGitMacheteBranch> getUpstreamBranch() {
    return Optional.ofNullable(upstreamBranch);
  }

  private void setUpstreamBranch(GitMacheteBranch givenUpstreamBranch) {
    assert upstreamBranch == null : "upstreamBranch has already been set";
    upstreamBranch = givenUpstreamBranch;
  }

  @Override
  public List<IGitMacheteBranch> getDownstreamBranches() {
    return List.narrow(downstreamBranches);
  }

  @Override
  public List<IGitMacheteCommit> getCommits() {
    return List.narrow(commits);
  }

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
