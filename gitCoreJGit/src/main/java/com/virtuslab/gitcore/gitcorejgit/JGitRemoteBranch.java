package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.IGitCoreRemoteBranch;

public class JGitRemoteBranch extends JGitBranch implements IGitCoreRemoteBranch {
  public static String branchesPath = "refs/remotes/";

  public JGitRemoteBranch(JGitRepository repo, String branchName) {
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
    return getBranchTypeString(false);
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    if (capitalized) return "Remote";
    else return "remote";
  }
}
