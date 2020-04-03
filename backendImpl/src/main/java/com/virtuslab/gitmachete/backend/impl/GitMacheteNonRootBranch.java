package com.virtuslab.gitmachete.backend.impl;

import java.util.Optional;

import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

@Getter
@ToString
public class GitMacheteNonRootBranch extends BaseGitMacheteNonRootBranch {
  private final String name;
  @ToString.Exclude
  @MonotonicNonNull
  private BaseGitMacheteBranch upstreamBranch = null;
  private final List<GitMacheteNonRootBranch> downstreamBranches;
  private final GitMacheteCommit pointedCommit;
  private final List<GitMacheteCommit> commits;
  private final SyncToOriginStatus syncToOriginStatus;
  private final SyncToParentStatus syncToParentStatus;
  private final IGitCoreLocalBranch coreLocalBranch;
  @Nullable
  private final String customAnnotation;

  @SuppressWarnings("argument.type.incompatible")
  public GitMacheteNonRootBranch(String name, List<GitMacheteNonRootBranch> downstreamBranches,
      GitMacheteCommit pointedCommit, List<GitMacheteCommit> commits,
      SyncToOriginStatus syncToOriginStatus, SyncToParentStatus syncToParentStatus,
      IGitCoreLocalBranch coreLocalBranch, @Nullable String customAnnotation) {
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
    for (GitMacheteNonRootBranch branch : downstreamBranches) {
      branch.setUpstreamBranch(this);
    }
  }

  @Override
  public BaseGitMacheteBranch getUpstreamBranch() {
    assert upstreamBranch != null : "upstreamBranch hasn't been set yet";
    return upstreamBranch;
  }

  void setUpstreamBranch(BaseGitMacheteBranch givenUpstreamBranch) {
    assert upstreamBranch == null : "upstreamBranch has already been set";
    upstreamBranch = givenUpstreamBranch;
  }

  @Override
  public List<BaseGitMacheteNonRootBranch> getDownstreamBranches() {
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
    return Try
        .of(coreLocalBranch::deriveForkPoint)
        .getOrElseThrow(e -> new GitMacheteException(e))
        .map(GitMacheteCommit::new);
  }
}
