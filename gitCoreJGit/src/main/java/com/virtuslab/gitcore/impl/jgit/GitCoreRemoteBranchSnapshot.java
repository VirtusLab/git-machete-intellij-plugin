package com.virtuslab.gitcore.impl.jgit;

import io.vavr.collection.List;
import lombok.Getter;

import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;

public class GitCoreRemoteBranchSnapshot extends BaseGitCoreBranchSnapshot implements IGitCoreRemoteBranchSnapshot {

  @Getter
  private final String remoteName;

  public GitCoreRemoteBranchSnapshot(
      String shortName,
      GitCoreCommit pointedCommit,
      List<IGitCoreReflogEntry> reflog,
      String remoteName) {
    super(shortName, pointedCommit, reflog);
    this.remoteName = remoteName;
  }

  @Override
  public String getName() {
    return BranchFullNameUtils.getRemoteBranchName(remoteName, shortName);
  }

  @Override
  public String getFullName() {
    return BranchFullNameUtils.getRemoteBranchFullName(remoteName, shortName);
  }

  @Override
  public String getFullNameAsLocalBranchOnRemote() {
    return BranchFullNameUtils.getRemoteBranchFullNameAsLocalBranchOnRemote(shortName);
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Remote" : "remote";
  }
}
