package com.virtuslab.gitmachete.frontend.defs;

import java.awt.Color;

import com.intellij.ui.JBColor;

public final class Colors {
  private Colors() {}

  private static final Color RED_COLOR = Color.decode("#FF0000");
  private static final Color ORANGE_COLOR = Color.decode("#FDA909");
  private static final Color DARK_ORANGE_COLOR = Color.decode("#D68C00");
  private static final Color YELLOW_COLOR = Color.decode("#C4A000");
  private static final Color GREEN_COLOR = Color.decode("#008000");
  private static final Color GRAY_COLOR = Color.decode("#AAAAAA");
  private static final Color DARK_GREY_COLOR = Color.decode("#555555");
  private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, /* alpha */ 0);

  /**
   * {@link com.intellij.ui.JBColor} are pairs of colors for light and dark IDE theme. LHS colors names are their
   * "general" description. They may differ from RHS (which are actual and specific colors).
   */
  public static final JBColor RED = new JBColor(RED_COLOR, RED_COLOR);
  public static final JBColor ORANGE = new JBColor(DARK_ORANGE_COLOR, ORANGE_COLOR);
  public static final JBColor YELLOW = new JBColor(YELLOW_COLOR, YELLOW_COLOR);
  public static final JBColor GREEN = new JBColor(GREEN_COLOR, GREEN_COLOR);
  public static final JBColor GRAY = new JBColor(DARK_GREY_COLOR, GRAY_COLOR);
  public static final JBColor TRANSPARENT = new JBColor(TRANSPARENT_COLOR, TRANSPARENT_COLOR);
}
