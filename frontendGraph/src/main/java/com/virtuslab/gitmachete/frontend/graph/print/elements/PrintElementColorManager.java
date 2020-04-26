package com.virtuslab.gitmachete.frontend.graph.print.elements;

import lombok.AllArgsConstructor;

import com.virtuslab.gitmachete.frontend.graph.api.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;

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
