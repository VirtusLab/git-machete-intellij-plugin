package com.virtuslab.gitmachete.frontend.graph.coloring;

import java.util.Map;

import com.intellij.ui.JBColor;

public final class GraphEdgeColorToJBColorMapper {

  private GraphEdgeColorToJBColorMapper() {}

  private static final Map<GraphEdgeColor, JBColor> COLORS = Map.of(
      GraphEdgeColor.GRAY, ColorDefinitions.GRAY,
      GraphEdgeColor.YELLOW, ColorDefinitions.YELLOW,
      GraphEdgeColor.RED, ColorDefinitions.RED,
      GraphEdgeColor.GREEN, ColorDefinitions.GREEN);

  public static JBColor getColor(GraphEdgeColor graphEdgeColor) {
    return COLORS.getOrDefault(graphEdgeColor, ColorDefinitions.TRANSPARENT);
  }

  public static JBColor getColor(int colorId) {
    return getColor(GraphEdgeColor.getById(colorId));
  }
}
