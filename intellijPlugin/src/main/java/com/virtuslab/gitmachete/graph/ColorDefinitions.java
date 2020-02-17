package com.virtuslab.gitmachete.graph;

import com.intellij.ui.JBColor;
import java.awt.Color;

public final class ColorDefinitions {
  private static final Color red = Color.decode("#FF0000");
  private static final Color orange = Color.decode("#FDA909");
  private static final Color dark_orange = Color.decode("#D68C00");
  private static final Color yellow = Color.decode("#C4A000");
  private static final Color green = Color.decode("#008000");
  private static final Color gray = Color.decode("#AAAAAA");
  private static final Color dark_gray = Color.decode("#555555");
  public static final Color transparent = new Color(0, 0, 0, 0);

  /*
   * JBColor are pairs of colors for light and dark IDE theme.
   * Lhs colors names are their "general" description.
   * They may differ from rhs (which are actual and specific colors).
   */
  public static final JBColor RED = new JBColor(red, red);
  public static final JBColor ORANGE = new JBColor(dark_orange, orange);
  public static final JBColor YELLOW = new JBColor(yellow, yellow);
  public static final JBColor GREEN = new JBColor(green, green);
  public static final JBColor GRAY = new JBColor(dark_gray, gray);
  public static final JBColor TRANSPARENT = new JBColor(transparent, transparent);
}
