package com.virtuslab.gitmachete.graph.facade;

import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.api.printer.PrintElementManager;
import com.intellij.vcs.log.graph.impl.print.GraphElementComparatorByLayoutIndex;
import com.intellij.vcs.log.graph.impl.print.elements.PrintElementWithGraphElement;
import com.virtuslab.gitmachete.graph.IGraphColorManager;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import javax.annotation.Nonnull;

public class GraphElementManager implements PrintElementManager {
  @Nonnull private final Comparator<GraphElement> graphElementComparator;
  @Nonnull private final ColorGetterByLayoutIndex colorGetterByLayoutIndex;
  @Nonnull private final LinearGraph linearGraph;
  @Nonnull private final Set<Integer> selectedNodesIds = Collections.emptySet();

  public GraphElementManager(
      @Nonnull LinearGraph linearGraph, @Nonnull IGraphColorManager colorManager) {
    this.linearGraph = linearGraph;
    colorGetterByLayoutIndex = new ColorGetterByLayoutIndex(colorManager);
    graphElementComparator =
        new GraphElementComparatorByLayoutIndex(linearGraph::getNodeId).reversed();
  }

  @Override
  public boolean isSelected(@Nonnull PrintElementWithGraphElement printElement) {
    GraphElement graphElement = printElement.getGraphElement();
    if (graphElement instanceof GraphNode) {
      int nodeId = linearGraph.getNodeId(((GraphNode) graphElement).getNodeIndex());
      return selectedNodesIds.contains(nodeId);
    }
    if (graphElement instanceof GraphEdge) {
      GraphEdge edge = (GraphEdge) graphElement;
      boolean selected =
          edge.getTargetId() == null || selectedNodesIds.contains(edge.getTargetId());
      selected &=
          edge.getUpNodeIndex() == null
              || selectedNodesIds.contains(linearGraph.getNodeId(edge.getUpNodeIndex()));
      selected &=
          edge.getDownNodeIndex() == null
              || selectedNodesIds.contains(linearGraph.getNodeId(edge.getDownNodeIndex()));
      return selected;
    }

    return false;
  }

  @Override
  public int getColorId(@Nonnull GraphElement element) {
    return colorGetterByLayoutIndex.getColorId(element);
  }

  @Nonnull
  @Override
  public Comparator<GraphElement> getGraphElementComparator() {
    return graphElementComparator;
  }
}
