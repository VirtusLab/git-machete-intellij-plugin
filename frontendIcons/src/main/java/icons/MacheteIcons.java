package icons;

import javax.swing.Icon;

import com.intellij.openapi.util.IconLoader;

/**
 * Icons must must be in package named "icons" based on
 * https://www.jetbrains.org/intellij/sdk/docs/reference_guide/work_with_icons_and_images.html
 */
public final class MacheteIcons {
  private MacheteIcons() {}

  public static final Icon FAST_FORWARD = IconLoader.getIcon("/applyNotConflictsLeft.svg");

  public static final Icon FETCH = IconLoader.getIcon("/download.svg");

  public static final Icon HELP = IconLoader.getIcon("/help.svg");

  public static final Icon MACHETE_FILE = IconLoader.getIcon("/macheteFileIcon.svg");

  public static final Icon MACHETE_LOAD_CHANGES = IconLoader.getIcon("/macheteLoadChanges.svg");

  public static final Icon PULL = IconLoader.getIcon("/incomingChangesOn.svg");

  public static final Icon PUSH = IconLoader.getIcon("/outgoingChangesOn.svg");

  public static final Icon REBASE = IconLoader.getIcon("/menu-cut.svg");

  public static final Icon REFRESH_STATUS = IconLoader.getIcon("/refresh.svg");

  public static final Icon RESET = IconLoader.getIcon("/reset.svg");

  public static final Icon TEXT_FILE = IconLoader.getIcon("/text.svg");

  public static final Icon TOGGLE_LISTING_COMMITS = IconLoader.getIcon("/showHiddens.svg");

  public static final Icon SLIDE_IN = IconLoader.getIcon("/branch.svg");

  public static final Icon SLIDE_OUT = IconLoader.getIcon("/gc.svg");
}
