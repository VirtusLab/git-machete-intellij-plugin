package com.virtuslab.gitmachete.graph.facade;

import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.printer.PrintElementManager;
import com.intellij.vcs.log.graph.impl.print.GraphElementComparatorByLayoutIndex;
import com.intellij.vcs.log.graph.impl.print.elements.PrintElementWithGraphElement;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;
import java.util.Comparator;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class GraphElementManager implements PrintElementManager {
  @Getter @Nonnull private final Comparator<GraphElement> graphElementComparator;
  @Nonnull private final ColorGetterByLayoutIndex colorGetterByLayoutIndex;

  public GraphElementManager(@Nonnull RepositoryGraph repositoryGraph) {
    colorGetterByLayoutIndex = new ColorGetterByLayoutIndex(repositoryGraph);
    graphElementComparator =
        new GraphElementComparatorByLayoutIndex(repositoryGraph::getNodeId).reversed();
  }

  @Override
  public boolean isSelected(@NotNull PrintElementWithGraphElement printElement) {
    return false;
  }

  @Override
  public int getColorId(@Nonnull GraphElement element) {
    return colorGetterByLayoutIndex.getColorId(element);
  }
}
