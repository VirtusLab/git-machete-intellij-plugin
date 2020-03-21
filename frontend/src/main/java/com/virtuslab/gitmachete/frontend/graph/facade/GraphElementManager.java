package com.virtuslab.gitmachete.frontend.graph.facade;

import java.util.Comparator;

import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.printer.PrintElementManager;
import com.intellij.vcs.log.graph.impl.print.GraphElementComparatorByLayoutIndex;
import com.intellij.vcs.log.graph.impl.print.elements.PrintElementWithGraphElement;
import lombok.Getter;

import com.virtuslab.gitmachete.frontend.graph.repositorygraph.RepositoryGraph;

public class GraphElementManager implements PrintElementManager {
  @Getter
  private final Comparator<GraphElement> graphElementComparator;
  private final ColorGetterByLayoutIndex colorGetterByLayoutIndex;

  public GraphElementManager(RepositoryGraph repositoryGraph) {
    colorGetterByLayoutIndex = new ColorGetterByLayoutIndex(repositoryGraph);
    graphElementComparator = new GraphElementComparatorByLayoutIndex(repositoryGraph::getNodeId).reversed();
  }

  @Override
  public boolean isSelected(PrintElementWithGraphElement printElement) {
    return false;
  }

  @Override
  public int getColorId(GraphElement element) {
    return colorGetterByLayoutIndex.getColorId(element);
  }
}
