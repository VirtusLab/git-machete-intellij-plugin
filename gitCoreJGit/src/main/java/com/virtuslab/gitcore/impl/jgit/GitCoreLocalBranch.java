package com.virtuslab.gitcore.impl.jgit;

import io.vavr.Lazy;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.eclipse.jgit.annotations.Nullable;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreReflogEntry;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;

@CustomLog
public class GitCoreLocalBranch extends BaseGitCoreBranch implements IGitCoreLocalBranch {

  @Nullable
  private final IGitCoreRemoteBranch remoteBranch;

  public GitCoreLocalBranch(
      String shortBranchName,
      Lazy<Either<GitCoreException, GitCoreCommit>> pointedCommit,
      Lazy<Either<GitCoreException, List<IGitCoreReflogEntry>>> reflog,
      @Nullable IGitCoreRemoteBranch remoteBranch) {
    super(shortBranchName, pointedCommit, reflog);
    this.remoteBranch = remoteBranch;
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
