package com.virtuslab.gitmachete.frontend.graph.print;

import static com.virtuslab.gitmachete.frontend.graph.print.elements.api.IEdgePrintElement.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.intellij.util.SmartList;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.GraphElementManager;
import com.virtuslab.gitmachete.frontend.graph.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.print.elements.impl.EdgePrintElement;
import com.virtuslab.gitmachete.frontend.graph.print.elements.impl.NodePrintElement;
import com.virtuslab.gitmachete.frontend.graph.print.elements.impl.PrintElementWithGraphElement;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;

@RequiredArgsConstructor
public final class PrintElementGenerator implements IPrintElementGenerator {
  private final RepositoryGraph repositoryGraph;
  private final GraphElementManager graphElementManager;

  @Override
  public Collection<PrintElementWithGraphElement> getPrintElements(int rowIndex) {
    assert rowIndex >= 0 : "Row index less than 0";
    PrintElementBuilder builder = new PrintElementBuilder(rowIndex);
    collectElements(rowIndex, builder);
    return builder.build();
  }

  private void collectElements(@NonNegative int rowIndex, PrintElementBuilder builder) {
    IGraphItem graphItem = repositoryGraph.getGraphItem(rowIndex);
    int position = graphItem.getIndentLevel();

    repositoryGraph.getVisibleEdgesWithPositions(rowIndex).forEach(edgeAndPos -> {
      builder.consumeUpEdge(edgeAndPos._1(), edgeAndPos._2());
      builder.consumeDownEdge(edgeAndPos._1(), edgeAndPos._2());
    });

    java.util.List<GraphEdge> adjacentEdges = repositoryGraph.getAdjacentEdges(rowIndex, EdgeFilter.ALL);
    for (GraphEdge edge : adjacentEdges) {
      Integer downNodeIndex = edge.getDownNodeIndex();
      Integer upNodeIndex = edge.getUpNodeIndex();
      if (downNodeIndex != null && downNodeIndex == rowIndex) {
        builder.consumeUpEdge(edge, position);
      }
      if (upNodeIndex != null && upNodeIndex == rowIndex) {
        builder.consumeDownEdge(edge, position);
      }
    }

    int nodeAndItsDownEdgePos = position;
    if (graphItem.isBranchItem() && !graphItem.asBranchItem().getBranch().isRootBranch()) {
      builder.consumeRightEdge(new GraphEdge(rowIndex, rowIndex), position);
      nodeAndItsDownEdgePos++;
    }

    builder.consumeNode(new GraphNode(rowIndex), nodeAndItsDownEdgePos);
    if (graphItem.hasChildItem()) {
      builder.consumeDownEdge(new GraphEdge(rowIndex, rowIndex + 1, /* targetId */ null, GraphEdgeType.USUAL),
          nodeAndItsDownEdgePos);
    }
  }

  @RequiredArgsConstructor
  private final class PrintElementBuilder {
    private final List<PrintElementWithGraphElement> edges = new ArrayList<>();
    private final List<PrintElementWithGraphElement> nodes = new SmartList<>();
    @NonNegative
    private final int rowIndex;

    public void consumeNode(GraphNode node, @NonNegative int position) {
      nodes.add(new NodePrintElement(rowIndex, position, node, graphElementManager));
    }

    public void consumeDownEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(new EdgePrintElement(rowIndex, position, Type.DOWN, edge, graphElementManager));
    }

    public void consumeUpEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(new EdgePrintElement(rowIndex, position, Type.UP, edge, graphElementManager));
    }

    public void consumeRightEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(new EdgePrintElement(rowIndex, position, Type.RIGHT, edge, graphElementManager));
    }

    public Collection<PrintElementWithGraphElement> build() {
      List<PrintElementWithGraphElement> result = new ArrayList<>(edges);
      result.addAll(nodes);
      return result;
    }
  }
}
