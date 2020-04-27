package com.virtuslab.gitmachete.frontend.graph.api.coloring;

import com.intellij.ui.JBColor;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

public final class GraphItemColorToJBColorMapper {

  private GraphItemColorToJBColorMapper() {}

  private static final Map<GraphItemColor, JBColor> COLORS = HashMap.of(
      GraphItemColor.GRAY, ColorDefinitions.GRAY,
      GraphItemColor.YELLOW, ColorDefinitions.YELLOW,
      GraphItemColor.RED, ColorDefinitions.RED,
      GraphItemColor.GREEN, ColorDefinitions.GREEN);

  public static JBColor getColor(GraphItemColor graphItemColor) {
    return COLORS.getOrElse(graphItemColor, ColorDefinitions.TRANSPARENT);
  }

  public static JBColor getColor(int colorId) {
    return getColor(GraphItemColor.getById(colorId));
  }
}
