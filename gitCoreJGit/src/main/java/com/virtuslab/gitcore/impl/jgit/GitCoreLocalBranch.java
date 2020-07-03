package com.virtuslab.gitcore.impl.jgit;

import io.vavr.collection.List;
import io.vavr.control.Option;
import org.eclipse.jgit.annotations.Nullable;

import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;

public class GitCoreLocalBranch extends BaseGitCoreBranch implements IGitCoreLocalBranch {

  private @Nullable final IGitCoreRemoteBranch remoteBranch;

  public GitCoreLocalBranch(
      String shortBranchName,
      GitCoreCommit pointedCommit,
      List<IGitCoreReflogEntry> reflog,
      @Nullable IGitCoreRemoteBranch remoteBranch) {
    super(shortBranchName, pointedCommit, reflog);
    this.remoteBranch = remoteBranch;
  }

  @Override
  public String getName() {
    return shortName;
  }

  @Override
  public String getFullName() {
    return BranchFullNameUtils.getLocalBranchFullName(shortName);
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Local" : "local";
  }

  @Override
  public Option<IGitCoreRemoteBranch> getRemoteTrackingBranch() {
    return Option.of(remoteBranch);
  }
}
