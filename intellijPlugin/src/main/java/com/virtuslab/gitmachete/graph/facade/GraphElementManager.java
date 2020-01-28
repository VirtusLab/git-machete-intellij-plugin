package com.virtuslab.gitmachete.graph.facade;

import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.api.printer.PrintElementManager;
import com.intellij.vcs.log.graph.impl.print.GraphElementComparatorByLayoutIndex;
import com.intellij.vcs.log.graph.impl.print.elements.PrintElementWithGraphElement;
import com.virtuslab.gitmachete.graph.repositorygraph.BaseRepositoryGraph;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Getter;

public class GraphElementManager implements PrintElementManager {
  @Getter @Nonnull private final Comparator<GraphElement> graphElementComparator;
  @Nonnull private final ColorGetterByLayoutIndex colorGetterByLayoutIndex;
  @Nonnull private final BaseRepositoryGraph repositoryGraph;
  @Nonnull private final Set<Integer> selectedNodesIds = Collections.emptySet();

  public GraphElementManager(@Nonnull BaseRepositoryGraph repositoryGraph) {
    this.repositoryGraph = repositoryGraph;
    colorGetterByLayoutIndex = new ColorGetterByLayoutIndex(repositoryGraph);
    graphElementComparator =
        new GraphElementComparatorByLayoutIndex(repositoryGraph::getNodeId).reversed();
  }

  @Override
  public boolean isSelected(@Nonnull PrintElementWithGraphElement printElement) {
    GraphElement graphElement = printElement.getGraphElement();
    if (graphElement instanceof GraphNode) {
      int nodeId = repositoryGraph.getNodeId(((GraphNode) graphElement).getNodeIndex());
      return selectedNodesIds.contains(nodeId);
    }
    if (graphElement instanceof GraphEdge) {
      GraphEdge edge = (GraphEdge) graphElement;
      boolean selected =
          edge.getTargetId() == null || selectedNodesIds.contains(edge.getTargetId());
      selected &=
          edge.getUpNodeIndex() == null
              || selectedNodesIds.contains(repositoryGraph.getNodeId(edge.getUpNodeIndex()));
      selected &=
          edge.getDownNodeIndex() == null
              || selectedNodesIds.contains(repositoryGraph.getNodeId(edge.getDownNodeIndex()));
      return selected;
    }

    return false;
  }

  @Override
  public int getColorId(@Nonnull GraphElement element) {
    return colorGetterByLayoutIndex.getColorId(element);
  }
}
