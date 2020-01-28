package com.virtuslab.gitmachete.graph;

import com.intellij.ui.JBColor;
import com.intellij.vcs.log.paint.ColorGenerator;
import java.awt.Color;
import java.util.Map;
import java.util.TreeMap;

public class DefaultColorGenerator implements ColorGenerator {
  private static final Map<Integer, JBColor> colorMap = new TreeMap<>();

  public static final Color gray = Color.decode("#AAAAAA");
  public static final Color dark_gray = Color.decode("#555555");
  public static final Color yellow = Color.decode("#C4A000");
  public static final Color red = Color.decode("#CD0000");
  public static final Color orange = Color.decode("#7F0000");
  public static final Color green = Color.decode("#008000");

  public static final JBColor BLACK = new JBColor(dark_gray, gray);
  public static final JBColor YELLOW = new JBColor(yellow, yellow);
  public static final JBColor RED = new JBColor(red, red);
  public static final JBColor ORANGE = new JBColor(orange, orange);
  public static final JBColor GREEN = new JBColor(green, green);

  static {
    colorMap.put(0, BLACK);
    colorMap.put(1, YELLOW);
    colorMap.put(2, RED);
    colorMap.put(3, GREEN);
  }

  @Override
  public Color getColor(int colorId) {
    if (colorId < 0 || colorId >= colorMap.size()) {
      return JBColor.GRAY;
    }

    return colorMap.get(colorId);
  }
}
