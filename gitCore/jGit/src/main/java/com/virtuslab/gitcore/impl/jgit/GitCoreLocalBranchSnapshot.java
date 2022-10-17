package com.virtuslab.gitcore.impl.jgit;

import static com.virtuslab.gitcore.impl.jgit.BranchFullNameUtils.getLocalBranchFullName;

import io.vavr.collection.List;
import org.eclipse.jgit.annotations.Nullable;

import com.virtuslab.gitcore.api.IGitCoreLocalBranchSnapshot;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranchSnapshot;

public class GitCoreLocalBranchSnapshot extends BaseGitCoreBranchSnapshot implements IGitCoreLocalBranchSnapshot {

  private @Nullable final IGitCoreRemoteBranchSnapshot remoteBranch;

  public GitCoreLocalBranchSnapshot(
      String shortBranchName,
      GitCoreCommit pointedCommit,
      List<IGitCoreReflogEntry> reflog,
      @Nullable IGitCoreRemoteBranchSnapshot remoteBranch) {
    super(shortBranchName, pointedCommit, reflog);
    this.remoteBranch = remoteBranch;
  }

  @Override
  public String getName() {
    return shortName;
  }

  @Override
  public String getFullName() {
    return getLocalBranchFullName(shortName);
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Local" : "local";
  }

  @Override
  public @Nullable IGitCoreRemoteBranchSnapshot getRemoteTrackingBranch() {
    return remoteBranch;
  }
}
