package com.virtuslab.gitcore.impl.jgit;

import org.eclipse.jgit.lib.Constants;

final class BranchFullNameUtils {
  private BranchFullNameUtils() {}

  static String getLocalBranchFullName(String localBranchName) {
    return Constants.R_HEADS + localBranchName;
  }

  static String getRemoteBranchName(String remoteName, String remoteBranchShortName) {
    return remoteName + "/" + remoteBranchShortName;
  }

  static String getRemoteBranchFullName(String remoteName, String remoteBranchShortName) {
    return Constants.R_REMOTES + getRemoteBranchName(remoteName, remoteBranchShortName);
  }

  static String getRemoteBranchFullNameAsLocalBranchOnRemote(String remoteBranchShortName) {
    return Constants.R_HEADS + remoteBranchShortName;
  }
}
