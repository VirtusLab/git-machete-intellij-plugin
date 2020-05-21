package com.virtuslab.gitcore.impl.jgit;

import lombok.Getter;
import org.eclipse.jgit.lib.Constants;

import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;

public class GitCoreRemoteBranch extends BaseGitCoreBranch implements IGitCoreRemoteBranch {

  @Getter
  private final String remoteName;

  public GitCoreRemoteBranch(GitCoreRepository repo, String remoteName, String shortName) {
    super(repo, shortName);
    this.remoteName = remoteName;
  }

  @Override
  public String getFullName() {
    return Constants.R_REMOTES + remoteName + "/" + shortName;
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Remote" : "remote";
  }
}
