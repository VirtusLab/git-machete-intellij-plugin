package com.virtuslab.gitmachete.graph;

import static com.virtuslab.gitmachete.graph.ColorDefinitions.GRAY;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.GREEN;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.RED;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.TRANSPARENT;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.YELLOW;

import com.intellij.ui.JBColor;
import com.intellij.vcs.log.paint.ColorGenerator;
import java.awt.Color;
import java.util.Map;

public class GraphEdgeColorEdgeJBColorGenerator implements ColorGenerator {

  private static final Map<GraphEdgeColor, JBColor> colors =
      Map.of(
          GraphEdgeColor.GRAY, GRAY,
          GraphEdgeColor.YELLOW, YELLOW,
          GraphEdgeColor.RED, RED,
          GraphEdgeColor.GREEN, GREEN);

  public static Color getColor(GraphEdgeColor graphEdgeColor) {
    return colors.getOrDefault(graphEdgeColor, TRANSPARENT);
  }

  @Override
  public Color getColor(int colorId) {
    return colors.getOrDefault(GraphEdgeColor.getById(colorId), TRANSPARENT);
  }
}
