package com.virtuslab.gitcore.impl.jgit;

import org.eclipse.jgit.lib.Constants;

public final class BranchFullNameUtils {
  private BranchFullNameUtils() {}

  public static String getLocalBranchFullName(String localBranchShortName) {
    return Constants.R_HEADS + localBranchShortName;
  }

  public static String getRemoteBranchFullName(String remoteName, String remoteBranchShortName) {
    return Constants.R_REMOTES + remoteName + "/" + remoteBranchShortName;
  }
}
