package com.virtuslab.gitmachete.graph.impl.facade;

import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.api.printer.PrintElementManager;
import com.intellij.vcs.log.graph.impl.print.GraphElementComparatorByLayoutIndex;
import com.intellij.vcs.log.graph.impl.print.elements.PrintElementWithGraphElement;
import com.virtuslab.gitmachete.api.IGraphColorManager;
import com.virtuslab.gitmachete.graph.impl.print.ColorGetterByLayoutIndex;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PrintElementManagerImpl implements PrintElementManager {
  @Nonnull private final Comparator<GraphElement> myGraphElementComparator;
  @Nonnull private final ColorGetterByLayoutIndex myColorGetter;
  @Nonnull private final LinearGraph myLinearGraph;
  @Nonnull private Set<Integer> mySelectedNodeIds = Collections.emptySet();
  @Nullable private PrintElementWithGraphElement mySelectedPrintElement = null;

  public PrintElementManagerImpl(
      @Nonnull LinearGraph linearGraph, @Nonnull IGraphColorManager colorManager) {
    myLinearGraph = linearGraph;
    myColorGetter = new ColorGetterByLayoutIndex(colorManager);
    myGraphElementComparator =
        new GraphElementComparatorByLayoutIndex(linearGraph::getNodeId).reversed();
  }

  @Override
  public boolean isSelected(@Nonnull PrintElementWithGraphElement printElement) {
    if (printElement.equals(mySelectedPrintElement)) return true;

    GraphElement graphElement = printElement.getGraphElement();
    if (graphElement instanceof GraphNode) {
      int nodeId = myLinearGraph.getNodeId(((GraphNode) graphElement).getNodeIndex());
      return mySelectedNodeIds.contains(nodeId);
    }
    if (graphElement instanceof GraphEdge) {
      GraphEdge edge = (GraphEdge) graphElement;
      boolean selected =
          edge.getTargetId() == null || mySelectedNodeIds.contains(edge.getTargetId());
      selected &=
          edge.getUpNodeIndex() == null
              || mySelectedNodeIds.contains(myLinearGraph.getNodeId(edge.getUpNodeIndex()));
      selected &=
          edge.getDownNodeIndex() == null
              || mySelectedNodeIds.contains(myLinearGraph.getNodeId(edge.getDownNodeIndex()));
      return selected;
    }

    return false;
  }

  @Override
  public int getColorId(@Nonnull GraphElement element) {
    return myColorGetter.getColorId();
  }

  @Nonnull
  @Override
  public Comparator<GraphElement> getGraphElementComparator() {
    return myGraphElementComparator;
  }
}
