package com.virtuslab.gitmachete.frontend.icons;

import javax.swing.Icon;

import com.intellij.openapi.util.IconLoader;

/**
 * Icons must be in package named "icons" based on
 * https://www.jetbrains.org/intellij/sdk/docs/reference_guide/work_with_icons_and_images.html
 */
public final class MacheteIcons {
  private MacheteIcons() {}

  public static final Icon DISCOVER = loadIcon("/icons/macheteLogoIcon.svg");

  public static final Icon EDIT = loadIcon("/icons/edit.svg");

  public static final Icon FAST_FORWARD = loadIcon("/icons/fastForward.svg");

  public static final Icon FETCH = loadIcon("/icons/fetch.svg");

  public static final Icon HELP = loadIcon("/icons/help.svg");

  public static final Icon MACHETE_FILE = loadIcon("/icons/macheteLogoIcon.svg");

  public static final Icon MERGE_PARENT = loadIcon("/icons/merge.svg");

  public static final Icon OVERRIDE_FORK_POINT = loadIcon("/icons/overrideForkPoint.svg");

  public static final Icon PULL = loadIcon("/icons/pull.svg");

  public static final Icon PUSH = loadIcon("/icons/push.svg");

  public static final Icon REBASE = loadIcon("/icons/rebase.svg");

  public static final Icon REFRESH_STATUS = loadIcon("/icons/refresh.svg");

  public static final Icon RESET = loadIcon("/icons/reset.svg");

  public static final Icon SLIDE_IN = loadIcon("/icons/slideIn.svg");

  public static final Icon SLIDE_OUT = loadIcon("/icons/slideOut.svg");

  public static final Icon TOGGLE_LISTING_COMMITS = loadIcon("/icons/toggleListingCommits.svg");

  private static Icon loadIcon(String path) {
    return IconLoader.getIcon(path, MacheteIcons.class);
  }

}
