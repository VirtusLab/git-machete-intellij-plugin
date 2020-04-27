package com.virtuslab.gitmachete.frontend.graph.impl.print;

import lombok.AllArgsConstructor;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.impl.repository.RepositoryGraph;

@AllArgsConstructor
public class PrintElementColorManager {
  private final RepositoryGraph repositoryGraph;

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
