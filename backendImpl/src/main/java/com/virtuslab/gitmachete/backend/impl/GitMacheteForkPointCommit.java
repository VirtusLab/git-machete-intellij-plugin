package com.virtuslab.gitmachete.backend.impl;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.ToString;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteForkPointCommit;

@ToString(callSuper = true)
public class GitMacheteForkPointCommit extends GitMacheteCommit implements IGitMacheteForkPointCommit {

  @Getter
  private final List<String> branchesWhereFoundInReflog;

  public GitMacheteForkPointCommit(IGitCoreCommit coreCommit, List<String> branchesWhereFoundInReflog) {
    super(coreCommit);
    this.branchesWhereFoundInReflog = branchesWhereFoundInReflog;
  }
}
