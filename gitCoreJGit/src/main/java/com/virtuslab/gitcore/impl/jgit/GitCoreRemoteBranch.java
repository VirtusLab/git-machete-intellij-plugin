package com.virtuslab.gitcore.impl.jgit;

import org.eclipse.jgit.lib.Constants;

import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;

public class GitCoreRemoteBranch extends BaseGitCoreBranch implements IGitCoreRemoteBranch {
  public static final String BRANCHES_PATH = Constants.R_REMOTES;

  public GitCoreRemoteBranch(GitCoreRepository repo, String branchName, String remoteName) {
    super(repo, branchName, remoteName);
  }

  @Override
  public String getFullName() {
    return getBranchesPath() + remoteName + "/" + branchName;
  }

  @Override
  public String getBranchesPath() {
    return BRANCHES_PATH;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public String getBranchTypeString() {
    return getBranchTypeString(/* capitalized */ false);
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Remote" : "remote";
  }
}
