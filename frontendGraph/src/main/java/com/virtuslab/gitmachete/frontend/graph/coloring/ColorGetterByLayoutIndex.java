package com.virtuslab.gitmachete.frontend.graph.coloring;

import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.utils.LinearGraphUtils;
import com.intellij.vcs.log.graph.utils.NormalEdge;
import lombok.AllArgsConstructor;

import com.virtuslab.gitmachete.frontend.graph.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;

@AllArgsConstructor
public class ColorGetterByLayoutIndex {
  private final RepositoryGraph repositoryGraph;

  public int getColorId(GraphElement element) {
    int nodeIndex;
    if (element instanceof GraphNode) {
      nodeIndex = ((GraphNode) element).getNodeIndex();
    } else {
      GraphEdge edge = (GraphEdge) element;
      NormalEdge normalEdge = LinearGraphUtils.asNormalEdge(edge);
      if (normalEdge != null) {
        nodeIndex = normalEdge.down;
      } else {
        nodeIndex = LinearGraphUtils.getNotNullNodeIndex(edge);
      }
    }

    assert nodeIndex >= 0;
    IGraphElement graphElement = repositoryGraph.getGraphElement(nodeIndex);

    return graphElement.getGraphEdgeColor().getId();
  }
}
