package com.virtuslab.gitmachete.frontend.graph.impl.render;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.render.IRenderPartColorIdProvider;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;

public class RenderPartColorIdProvider implements IRenderPartColorIdProvider {
  @NotOnlyInitialized
  private final IRepositoryGraph repositoryGraph;

  public RenderPartColorIdProvider(@UnderInitialization IRepositoryGraph repositoryGraph) {
    this.repositoryGraph = repositoryGraph;
  }

  public int getColorId(IGraphElement element) {
    int nodeIndex;
    if (element.isNode()) {
      nodeIndex = element.asNode().getNodeIndex();
    } else { // isEdge
      nodeIndex = element.asEdge().getDownNodeIndex();
    }

    IGraphItem graphItem = repositoryGraph.getGraphItem(nodeIndex);

    return graphItem.getGraphItemColor().getId();
  }
}
