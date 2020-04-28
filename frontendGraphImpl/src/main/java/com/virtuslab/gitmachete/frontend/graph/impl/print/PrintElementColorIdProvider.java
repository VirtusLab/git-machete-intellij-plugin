package com.virtuslab.gitmachete.frontend.graph.impl.print;

import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import lombok.AllArgsConstructor;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.print.IPrintElementColorIdProvider;

@AllArgsConstructor
public class PrintElementColorIdProvider implements IPrintElementColorIdProvider {
  private final IRepositoryGraph repositoryGraph;

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
