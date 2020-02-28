package com.virtuslab.gitmachete.graph;

import static com.virtuslab.gitmachete.graph.ColorDefinitions.GRAY;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.GREEN;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.RED;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.TRANSPARENT;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.YELLOW;

import com.intellij.ui.JBColor;
import java.util.Map;

public final class GraphEdgeColorToJBColorMapper {

  private GraphEdgeColorToJBColorMapper() {}

  private static final Map<GraphEdgeColor, JBColor> colors =
      Map.of(
          GraphEdgeColor.GRAY, GRAY,
          GraphEdgeColor.YELLOW, YELLOW,
          GraphEdgeColor.RED, RED,
          GraphEdgeColor.GREEN, GREEN);

  public static JBColor getColor(GraphEdgeColor graphEdgeColor) {
    return colors.getOrDefault(graphEdgeColor, TRANSPARENT);
  }

  public static JBColor getColor(int colorId) {
    return getColor(GraphEdgeColor.getById(colorId));
  }
}
