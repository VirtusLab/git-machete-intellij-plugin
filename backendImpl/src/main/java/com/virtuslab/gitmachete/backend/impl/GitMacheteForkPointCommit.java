package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.ToString;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteForkPointCommit;

@ToString(callSuper = true)
public final class GitMacheteForkPointCommit extends GitMacheteCommit implements IGitMacheteForkPointCommit {

  @Getter(onMethod_ = {@Override})
  private final List<String> branchesContainingInReflog;

  @Getter(onMethod_ = {@Override})
  private final boolean isOverridden;

  private GitMacheteForkPointCommit(IGitCoreCommit coreCommit, List<String> branchesContainingInReflog, boolean isOverridden) {
    super(coreCommit);
    this.branchesContainingInReflog = branchesContainingInReflog;
    this.isOverridden = isOverridden;
  }

  public static GitMacheteForkPointCommit overridden(IGitCoreCommit overrideCoreCommit) {
    return new GitMacheteForkPointCommit(overrideCoreCommit, List.empty(), true);
  }

  public static GitMacheteForkPointCommit inferred(IGitCoreCommit coreCommit, List<String> branchesContainingInReflog) {
    return new GitMacheteForkPointCommit(coreCommit, branchesContainingInReflog, false);
  }

  public static GitMacheteForkPointCommit parentFallback(IGitCoreCommit parentCoreCommit) {
    return new GitMacheteForkPointCommit(parentCoreCommit, List.empty(), false);
  }
}
