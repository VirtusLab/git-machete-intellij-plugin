package com.virtuslab.gitmachete.testcommon;

public final class SetupScripts {
  private SetupScripts() {}
  public final static String SETUP_FOR_NO_REMOTES = "setup-with-no-remotes.sh";
  public final static String SETUP_WITH_SINGLE_REMOTE = "setup-with-single-remote.sh";
  public final static String SETUP_WITH_MULTIPLE_REMOTES = "setup-with-multiple-remotes.sh";
  public final static String SETUP_FOR_DIVERGED_AND_OLDER_THAN = "setup-for-diverged-and-older-than.sh";
  public final static String SETUP_FOR_YELLOW_EDGES = "setup-for-yellow-edges.sh";
  public final static String SETUP_FOR_OVERRIDDEN_FORK_POINT = "setup-for-overridden-fork-point.sh";

  public final static String[] ALL_SETUP_SCRIPTS = {
      SETUP_FOR_NO_REMOTES,
      SETUP_WITH_SINGLE_REMOTE,
      SETUP_WITH_MULTIPLE_REMOTES,
      SETUP_FOR_DIVERGED_AND_OLDER_THAN,
      SETUP_FOR_YELLOW_EDGES,
      SETUP_FOR_OVERRIDDEN_FORK_POINT
  };

}
