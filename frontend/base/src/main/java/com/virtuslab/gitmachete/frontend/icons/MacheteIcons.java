package com.virtuslab.gitmachete.frontend.icons;

import javax.swing.Icon;

import com.intellij.openapi.util.IconLoader;

public final class MacheteIcons {
  private MacheteIcons() {}

  public static final Icon DISCOVER = loadIcon("macheteLogoIcon");

  public static final Icon EDIT = loadIcon("edit");

  public static final Icon FAST_FORWARD = loadIcon("fastForward");

  public static final Icon FETCH = loadIcon("fetch");

  public static final Icon HELP = loadIcon("help");

  public static final Icon MACHETE_FILE = loadIcon("macheteLogoIcon");

  public static final Icon MERGE_PARENT = loadIcon("merge");

  public static final Icon OVERRIDE_FORK_POINT = loadIcon("overrideForkPoint");

  public static final Icon PULL = loadIcon("pull");

  public static final Icon PUSH = loadIcon("push");

  public static final Icon REBASE = loadIcon("rebase");

  public static final Icon REFRESH_STATUS = loadIcon("refresh");

  public static final Icon RENAME = loadIcon("rename");

  public static final Icon RESET = loadIcon("reset");

  public static final Icon SLIDE_IN = loadIcon("slideIn");

  public static final Icon SLIDE_OUT = loadIcon("slideOut");

  public static final Icon SQUASH = loadIcon("squash");

  public static final Icon TOGGLE_LISTING_COMMITS = loadIcon("toggleListingCommits");

  public static final Icon TRAVERSE = loadIcon("traverse");

  private static Icon loadIcon(String basename) {
    return IconLoader.getIcon("/${basename}.svg", MacheteIcons.class);
  }

}
