package com.virtuslab.gitmachete.graph;

import com.intellij.ui.JBColor;
import java.awt.Color;

public abstract class ColorDefinitions {
  protected static final Color gray = Color.decode("#AAAAAA");
  protected static final Color dark_gray = Color.decode("#555555");
  protected static final Color yellow = Color.decode("#C4A000");
  protected static final Color red = Color.decode("#FF0000");
  protected static final Color orange = Color.decode("#FDA909");
  protected static final Color dark_orange = Color.decode("#D68C00");
  protected static final Color green = Color.decode("#008000");

  /*
   * JBColor are pairs of colors for light and dark IDE theme.
   * Lhs colors names are their "general" description.
   * They may differ from rhs (which are actual and specific colors).
   */
  protected static final JBColor GRAY = new JBColor(dark_gray, gray);
  protected static final JBColor YELLOW = new JBColor(yellow, yellow);
  protected static final JBColor RED = new JBColor(red, red);
  protected static final JBColor ORANGE = new JBColor(dark_orange, orange);
  protected static final JBColor GREEN = new JBColor(green, green);
}
