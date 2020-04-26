package com.virtuslab.gitmachete.frontend.graph.coloring;

import lombok.AllArgsConstructor;

import com.virtuslab.gitmachete.frontend.graph.api.GraphEdge;
import com.virtuslab.gitmachete.frontend.graph.api.GraphNode;
import com.virtuslab.gitmachete.frontend.graph.api.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;

@AllArgsConstructor
public class ColorGetterByLayoutIndex {
  private final RepositoryGraph repositoryGraph;

  public int getColorId(IGraphElement element) {
    int nodeIndex;
    if (element instanceof GraphNode) {
      nodeIndex = ((GraphNode) element).getNodeIndex();
    } else {
      GraphEdge edge = (GraphEdge) element;
      nodeIndex = edge.getDownNodeIndex();
    }

    assert nodeIndex >= 0 : "Node index less than 0";
    IGraphItem graphItem = repositoryGraph.getGraphItem(nodeIndex);

    return graphItem.getGraphItemColor().getId();
  }
}
