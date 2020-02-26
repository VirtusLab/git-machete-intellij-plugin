package com.virtuslab.gitcore.impl.jgit;

import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;

public class GitCoreRemoteBranch extends GitCoreBranch implements IGitCoreRemoteBranch {
  public static final String branchesPath = "refs/remotes/";

  public GitCoreRemoteBranch(GitCoreRepository repo, String branchName) {
    super(repo, branchName);
  }

  @Override
  public String getBranchesPath() {
    return branchesPath;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public String getBranchTypeString() {
    return getBranchTypeString(/*capitalized*/ false);
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Remote" : "remote";
  }
}
