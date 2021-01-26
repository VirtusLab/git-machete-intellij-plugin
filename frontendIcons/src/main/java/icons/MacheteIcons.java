package icons;

import javax.swing.Icon;

import com.intellij.openapi.util.IconLoader;

/**
 * Icons must must be in package named "icons" based on
 * https://www.jetbrains.org/intellij/sdk/docs/reference_guide/work_with_icons_and_images.html
 */
public final class MacheteIcons {
  private MacheteIcons() {}

  public static final Icon DISCOVER = loadIcon("/macheteLogoIcon.svg");

  public static final Icon FAST_FORWARD = loadIcon("/applyNotConflictsLeft.svg");

  public static final Icon FETCH = loadIcon("/download.svg");

  public static final Icon HELP = loadIcon("/help.svg");

  public static final Icon MACHETE_FILE = loadIcon("/macheteLogoIcon.svg");

  public static final Icon MACHETE_LOAD_CHANGES = loadIcon("/macheteLoadChanges.svg");

  public static final Icon OVERRIDE_FORK_POINT = loadIcon("/overrideForkPoint.svg");

  public static final Icon PULL = loadIcon("/incomingChangesOn.svg");

  public static final Icon PUSH = loadIcon("/outgoingChangesOn.svg");

  public static final Icon REBASE = loadIcon("/menu-cut.svg");

  public static final Icon REFRESH_STATUS = loadIcon("/refresh.svg");

  public static final Icon RESET = loadIcon("/reset.svg");

  public static final Icon TEXT_FILE = loadIcon("/text.svg");

  public static final Icon TOGGLE_LISTING_COMMITS = loadIcon("/showHiddens.svg");

  public static final Icon SLIDE_IN = loadIcon("/branch.svg");

  public static final Icon SLIDE_OUT = loadIcon("/gc.svg");

  private static Icon loadIcon(String path) {
    return IconLoader.getIcon(path, MacheteIcons.class);
  }
}
