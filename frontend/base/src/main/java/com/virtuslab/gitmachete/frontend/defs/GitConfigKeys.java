package com.virtuslab.gitmachete.frontend.defs;

public final class GitConfigKeys {
  private GitConfigKeys() {}
  public static final String DELETE_LOCAL_BRANCH_ON_SLIDE_OUT = "machete.slideOut.deleteLocalBranch";
  public static String overrideForkPointToKey(String branchName) {
    return "machete.overrideForkPoint.${branchName}.to";
  }
  public static String overrideForkPointWhileDescendantOf(String branchName) {
    return "machete.overrideForkPoint.${branchName}.whileDescendantOf";
  }
}
