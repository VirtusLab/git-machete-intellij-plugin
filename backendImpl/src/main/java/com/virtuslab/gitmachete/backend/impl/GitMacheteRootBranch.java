package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;

@Getter
@ToString
@Slf4j(topic = "backend")
public final class GitMacheteRootBranch extends BaseGitMacheteRootBranch {
  private final String name;
  private final List<GitMacheteNonRootBranch> downstreamBranches;
  private final IGitMacheteCommit pointedCommit;
  private final ISyncToRemoteStatus syncToRemoteStatus;
  @Nullable
  private final String customAnnotation;

  public GitMacheteRootBranch(String name, List<GitMacheteNonRootBranch> downstreamBranches,
      IGitMacheteCommit pointedCommit,
      ISyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation) {
    log.debug(
        "Creating GitMacheteRootBranch(name = ${name}, downstreamBranches.length() = ${downstreamBranches.length()}"
            + "pointedCommit = ${pointedCommit.getHash()}, syncToRemoteStatus = ${syncToRemoteStatus}}, customAnnotation = ${customAnnotation}");
    this.name = name;
    this.downstreamBranches = downstreamBranches;
    this.pointedCommit = pointedCommit;
    this.syncToRemoteStatus = syncToRemoteStatus;
    this.customAnnotation = customAnnotation;

    // Note: since the class is final, `this` is already @Initialized at this point.

    // This is a hack necessary to create an immutable cyclic structure (children pointing at the parent & parent
    // pointing at the children).
    // This is definitely not the cleanest solution, but still easier to manage and reason about than keeping the
    // upstream data somewhere outside of GitMacheteBranch (e.g. in GitMacheteRepository).
    for (GitMacheteNonRootBranch branch : downstreamBranches) {
      log.debug("Set this (${name}) branch as upstream for ${branch.getName()}");
      branch.setUpstreamBranch(this);
    }
  }

  @Override
  public List<BaseGitMacheteNonRootBranch> getDownstreamBranches() {
    return List.narrow(downstreamBranches);
  }

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }
}
