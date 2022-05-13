package icons;

import javax.swing.Icon;

import com.intellij.openapi.util.IconLoader;

/**
 * Icons must be in package named "icons" based on
 * https://www.jetbrains.org/intellij/sdk/docs/reference_guide/work_with_icons_and_images.html
 */
public final class MacheteIcons {
  private MacheteIcons() {}

  public static final Icon DISCOVER = loadIcon("/macheteLogoIcon.svg");

  public static final Icon EDIT = loadIcon("/edit.svg");

  public static final Icon FAST_FORWARD = loadIcon("/applyNotConflictsLeft.svg");

  public static final Icon FETCH = loadIcon("/fetch.svg");

  public static final Icon HELP = loadIcon("/help.svg");

  public static final Icon MACHETE_FILE = loadIcon("/macheteLogoIcon.svg");

  public static final Icon MERGE_PARENT = loadIcon("/merge.svg");

  public static final Icon OVERRIDE_FORK_POINT = loadIcon("/overrideForkPoint.svg");

  public static final Icon PULL = loadIcon("/pull.svg");

  public static final Icon PUSH = loadIcon("/push.svg");

  public static final Icon REBASE = loadIcon("/menuCut.svg");

  public static final Icon REFRESH_STATUS = loadIcon("/refresh.svg");

  public static final Icon RESET = loadIcon("/reset.svg");

  public static final Icon SLIDE_IN = loadIcon("/slideIn.svg");

  public static final Icon SLIDE_OUT = loadIcon("/slideOut.svg");

  public static final Icon TOGGLE_LISTING_COMMITS = loadIcon("/toggleListingCommits.svg");

  private static Icon loadIcon(String path) {
    return IconLoader.getIcon(path, MacheteIcons.class);
  }
}
