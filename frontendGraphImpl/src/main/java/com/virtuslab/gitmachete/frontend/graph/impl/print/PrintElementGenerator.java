package com.virtuslab.gitmachete.frontend.graph.impl.print;

import java.util.ArrayList;

import com.intellij.util.SmartList;
import io.vavr.collection.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

import com.virtuslab.gitmachete.frontend.graph.api.elements.GraphEdge;
import com.virtuslab.gitmachete.frontend.graph.api.elements.GraphNode;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.print.IPrintElementColorIdProvider;
import com.virtuslab.gitmachete.frontend.graph.api.print.IPrintElementGenerator;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IEdgePrintElement;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.impl.print.elements.EdgePrintElement;
import com.virtuslab.gitmachete.frontend.graph.impl.print.elements.NodePrintElement;
import com.virtuslab.gitmachete.frontend.graph.impl.print.elements.PrintElementWithGraphElement;

public final class PrintElementGenerator implements IPrintElementGenerator {
  @NotOnlyInitialized
  private final IRepositoryGraph repositoryGraph;
  @NotOnlyInitialized
  private final IPrintElementColorIdProvider printElementColorIdProvider;

  public PrintElementGenerator(@UnderInitialization IRepositoryGraph repositoryGraph) {
    this.repositoryGraph = repositoryGraph;
    this.printElementColorIdProvider = new PrintElementColorIdProvider(repositoryGraph);
  }

  @Override
  public List<PrintElementWithGraphElement> getPrintElements(@NonNegative int rowIndex) {
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

    List<GraphEdge> adjacentEdges = repositoryGraph.getAdjacentEdges(rowIndex);
    for (GraphEdge edge : adjacentEdges) {
      int downNodeIndex = edge.getDownNodeIndex();
      int upNodeIndex = edge.getUpNodeIndex();
      if (downNodeIndex == rowIndex) {
        builder.consumeUpEdge(edge, position);
      }
      if (upNodeIndex == rowIndex) {
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
      builder.consumeDownEdge(new GraphEdge(rowIndex, rowIndex + 1), nodeAndItsDownEdgePos);
    }
  }

  @RequiredArgsConstructor
  private final class PrintElementBuilder {
    private final java.util.List<PrintElementWithGraphElement> edges = new ArrayList<>();
    private final java.util.List<PrintElementWithGraphElement> nodes = new SmartList<>();
    @NonNegative
    private final int rowIndex;

    public void consumeNode(GraphNode node, @NonNegative int position) {
      nodes.add(new NodePrintElement(rowIndex, position, node, printElementColorIdProvider));
    }

    public void consumeDownEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(
          new EdgePrintElement(rowIndex, position, IEdgePrintElement.Type.DOWN, edge, printElementColorIdProvider));
    }

    public void consumeUpEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(new EdgePrintElement(rowIndex, position, IEdgePrintElement.Type.UP, edge, printElementColorIdProvider));
    }

    public void consumeRightEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(
          new EdgePrintElement(rowIndex, position, IEdgePrintElement.Type.RIGHT, edge, printElementColorIdProvider));
    }

    public List<PrintElementWithGraphElement> build() {
      List<PrintElementWithGraphElement> result = List.ofAll(edges);
      return result.appendAll(nodes);
    }
  }
}
