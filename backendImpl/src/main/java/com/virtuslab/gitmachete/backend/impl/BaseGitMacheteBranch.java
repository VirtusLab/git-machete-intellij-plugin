package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

@Getter
public abstract class BaseGitMacheteBranch implements IGitMacheteBranch {
  private final String name;
  private final List<GitMacheteNonRootBranch> downstreamBranches;
  private final IGitMacheteCommit pointedCommit;
  private final SyncToRemoteStatus syncToRemoteStatus;
  private final @Nullable IGitMacheteRemoteBranch remoteBranch;
  private final @Nullable String customAnnotation;
  private final @Nullable String statusHookOutput;

  protected BaseGitMacheteBranch(
      String name,
      List<GitMacheteNonRootBranch> downstreamBranches,
      IGitMacheteCommit pointedCommit,
      @Nullable IGitMacheteRemoteBranch remoteBranch,
      SyncToRemoteStatus syncToRemoteStatus,
      @Nullable String customAnnotation,
      @Nullable String statusHookOutput) {
    this.name = name;
    this.downstreamBranches = downstreamBranches;
    this.pointedCommit = pointedCommit;
    this.syncToRemoteStatus = syncToRemoteStatus;
    this.remoteBranch = remoteBranch;
    this.customAnnotation = customAnnotation;
    this.statusHookOutput = statusHookOutput;
  }

  @Override
  public Option<String> getCustomAnnotation() {
    return Option.of(customAnnotation);
  }

  @Override
  public Option<String> getStatusHookOutput() {
    return Option.of(statusHookOutput);
  }

  @Override
  public Option<IGitMacheteRemoteBranch> getRemoteTrackingBranch() {
    return Option.of(remoteBranch);
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    return this == other;
  }

  @Override
  public final int hashCode() {
    return getName().hashCode() * 37 + getPointedCommit().hashCode();
  }
}
