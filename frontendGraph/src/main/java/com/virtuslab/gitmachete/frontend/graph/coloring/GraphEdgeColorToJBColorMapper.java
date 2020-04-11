package com.virtuslab.gitmachete.frontend.graph.coloring;

import com.intellij.ui.JBColor;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

public final class GraphEdgeColorToJBColorMapper {

  private GraphEdgeColorToJBColorMapper() {}

  private static final Map<GraphEdgeColor, JBColor> COLORS = HashMap.of(
      GraphEdgeColor.GRAY, ColorDefinitions.GRAY,
      GraphEdgeColor.YELLOW, ColorDefinitions.YELLOW,
      GraphEdgeColor.RED, ColorDefinitions.RED,
      GraphEdgeColor.GREEN, ColorDefinitions.GREEN);

  public static JBColor getColor(GraphEdgeColor graphEdgeColor) {
    return COLORS.getOrElse(graphEdgeColor, ColorDefinitions.TRANSPARENT);
  }

  public static JBColor getColor(int colorId) {
    return getColor(GraphEdgeColor.getById(colorId));
  }
}
