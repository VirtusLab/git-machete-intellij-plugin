package com.virtuslab.gitmachete.frontend.graph.impl.render;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;

public class GraphItemColorForGraphElementProvider {
  @NotOnlyInitialized
  private final IRepositoryGraph repositoryGraph;

  public GraphItemColorForGraphElementProvider(@UnderInitialization IRepositoryGraph repositoryGraph) {
    this.repositoryGraph = repositoryGraph;
  }

  public GraphItemColor getGraphItemColor(IGraphElement element) {
    int nodeIndex;
    if (element.isNode()) {
      nodeIndex = element.asNode().getNodeIndex();
    } else { // isEdge
      nodeIndex = element.asEdge().getDownNodeIndex();
    }

    return repositoryGraph.getGraphItem(nodeIndex).getColor();
  }
}
