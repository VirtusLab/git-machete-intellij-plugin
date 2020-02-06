package com.virtuslab.gitmachete.graph;

import com.intellij.ui.JBColor;
import com.intellij.vcs.log.paint.ColorGenerator;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.awt.Color;
import java.util.Map;

public class GitMacheteColorGenerator implements ColorGenerator {

  public static final Color gray = Color.decode("#AAAAAA");
  public static final Color dark_gray = Color.decode("#555555");
  public static final Color yellow = Color.decode("#C4A000");
  public static final Color red = Color.decode("#CD0000");
  public static final Color orange = Color.decode("#7F0000");
  public static final Color green = Color.decode("#008000");

  /*
   * JBColor are pairs of colors for light and dark IDE theme.
   * Lhs colors names are their "general" description.
   * They may differ from rhs (which are actual and specific colors).
   */
  public static final JBColor GRAY = new JBColor(dark_gray, gray);
  public static final JBColor YELLOW = new JBColor(yellow, yellow);
  public static final JBColor RED = new JBColor(red, red);
  public static final JBColor ORANGE = new JBColor(orange, orange);
  public static final JBColor GREEN = new JBColor(green, green);

  private static final Map<Integer, JBColor> gitMacheteColors =
      Map.of(
          SyncToParentStatus.Merged.getColorId(), GRAY,
          SyncToParentStatus.NotADirectDescendant.getColorId(), YELLOW,
          SyncToParentStatus.OutOfSync.getColorId(), RED,
          SyncToParentStatus.InSync.getColorId(), GREEN);

  @Override
  public Color getColor(int colorId) {
    return gitMacheteColors.getOrDefault(colorId, JBColor.GRAY);
  }
}
