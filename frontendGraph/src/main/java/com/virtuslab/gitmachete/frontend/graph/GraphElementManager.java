package com.virtuslab.gitmachete.frontend.graph;

import java.util.Comparator;

import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.impl.print.GraphElementComparatorByLayoutIndex;
import lombok.Getter;

import com.virtuslab.gitmachete.frontend.graph.coloring.ColorGetterByLayoutIndex;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;

public class GraphElementManager {
  @Getter
  private final Comparator<GraphElement> graphElementComparator;
  private final ColorGetterByLayoutIndex colorGetterByLayoutIndex;

  public GraphElementManager(RepositoryGraph repositoryGraph) {
    colorGetterByLayoutIndex = new ColorGetterByLayoutIndex(repositoryGraph);
    graphElementComparator = new GraphElementComparatorByLayoutIndex(repositoryGraph::getNodeId).reversed();
  }

  public int getColorId(GraphElement element) {
    return colorGetterByLayoutIndex.getColorId(element);
  }
}
