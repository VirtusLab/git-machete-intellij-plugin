package com.virtuslab.gitmachete.frontend.graph;

import lombok.Getter;

import com.virtuslab.gitmachete.frontend.graph.api.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.coloring.ColorGetterByLayoutIndex;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;

public class GraphElementManager {
  @Getter
  private final ColorGetterByLayoutIndex colorGetterByLayoutIndex;

  public GraphElementManager(RepositoryGraph repositoryGraph) {
    colorGetterByLayoutIndex = new ColorGetterByLayoutIndex(repositoryGraph);
  }

  public int getColorId(IGraphElement element) {
    return colorGetterByLayoutIndex.getColorId(element);
  }
}
