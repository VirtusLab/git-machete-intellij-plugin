package com.virtuslab.gitmachete.graph;

import com.intellij.ui.JBColor;
import java.awt.Color;

public interface IColorGenerator {
  Color red = Color.decode("#FF0000");
  Color orange = Color.decode("#FDA909");
  Color dark_orange = Color.decode("#D68C00");
  Color yellow = Color.decode("#C4A000");
  Color green = Color.decode("#008000");
  Color gray = Color.decode("#AAAAAA");
  Color dark_gray = Color.decode("#555555");

  /*
   * JBColor are pairs of colors for light and dark IDE theme.
   * Lhs colors names are their "general" description.
   * They may differ from rhs (which are actual and specific colors).
   */
  JBColor RED = new JBColor(red, red);
  JBColor ORANGE = new JBColor(dark_orange, orange);
  JBColor YELLOW = new JBColor(yellow, yellow);
  JBColor GREEN = new JBColor(green, green);
  JBColor GRAY = new JBColor(dark_gray, gray);
}
