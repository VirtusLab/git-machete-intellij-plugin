package com.virtuslab.gitcore.impl.jgit;

import io.vavr.Lazy;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.Getter;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;

public class GitCoreRemoteBranch extends BaseGitCoreBranch implements IGitCoreRemoteBranch {

  @Getter(onMethod_ = {@Override})
  private final String remoteName;

  public GitCoreRemoteBranch(
      String shortName,
      Lazy<Either<GitCoreException, GitCoreCommit>> pointedCommit,
      Lazy<Either<GitCoreException, List<IGitCoreReflogEntry>>> reflog,
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
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Remote" : "remote";
  }
}
