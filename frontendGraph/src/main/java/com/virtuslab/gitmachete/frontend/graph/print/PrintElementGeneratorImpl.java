package com.virtuslab.gitmachete.frontend.graph.print;

import java.util.ArrayList;
import java.util.Collection;

import com.intellij.vcs.log.graph.EdgePrintElement.Type;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.api.printer.PrintElementGenerator;
import com.intellij.vcs.log.graph.impl.print.elements.EdgePrintElementImpl;
import com.intellij.vcs.log.graph.impl.print.elements.PrintElementWithGraphElement;
import com.intellij.vcs.log.graph.impl.print.elements.SimplePrintElementImpl;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.GraphElementManager;
import com.virtuslab.gitmachete.frontend.graph.elements.BranchElement;
import com.virtuslab.gitmachete.frontend.graph.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;

public final class PrintElementGeneratorImpl implements PrintElementGenerator {
  private final RepositoryGraph repositoryGraph;
  private final GraphElementManager printElementManager;

  public PrintElementGeneratorImpl(RepositoryGraph repositoryGraph, GraphElementManager graphElementManager) {
    this.repositoryGraph = repositoryGraph;
    this.printElementManager = graphElementManager;
  }

  @Override
  public Collection<PrintElementWithGraphElement> getPrintElements(int rowIndex) {
    assert rowIndex >= 0;
    PrintElementBuilder builder = new PrintElementBuilder(rowIndex);
    collectElements(rowIndex, builder);
    return builder.build();
  }

  private void collectElements(@NonNegative int rowIndex, PrintElementBuilder builder) {
    GraphNode graphNode = repositoryGraph.getGraphNode(rowIndex);
    IGraphElement graphElement = repositoryGraph.getGraphElement(rowIndex);
    int position = graphElement.getIndentLevel();

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
    if (graphElement.isBranch() && !((BranchElement) graphElement).getBranch().isRootBranch()) {
      builder.consumeRightEdge(new GraphEdge(rowIndex, rowIndex, null, GraphEdgeType.USUAL), position);
      nodeAndItsDownEdgePos++;
    }

    builder.consumeNode(graphNode, nodeAndItsDownEdgePos);
    if (graphElement.hasSubelement()) {
      builder.consumeDownEdge(new GraphEdge(rowIndex, rowIndex + 1,  null, GraphEdgeType.USUAL),
          nodeAndItsDownEdgePos);
    }
  }

  @RequiredArgsConstructor
  private final class PrintElementBuilder {
    private final ArrayList<PrintElementWithGraphElement> result = new ArrayList<>();
    private final ArrayList<PrintElementWithGraphElement> nodes = new ArrayList<>();
    @NonNegative
    private final int rowIndex;

    public void consumeNode(GraphNode node, @NonNegative int position) {
      nodes.add(new SimplePrintElementImpl(rowIndex, position, node, printElementManager));
    }

    public void consumeDownEdge(GraphEdge edge, @NonNegative int position) {
      result.add(
          new EdgePrintElementImpl(rowIndex, position, position, Type.DOWN, edge, /* hasArrow */ false,
              printElementManager));
    }

    public void consumeUpEdge(GraphEdge edge, @NonNegative int position) {
      result.add(
          new EdgePrintElementImpl(rowIndex, position, position, Type.UP, edge, /* hasArrow */ false,
              printElementManager));
    }

    public void consumeRightEdge(GraphEdge edge, @NonNegative int position) {
      result.add(new RightEdgePrintElement(rowIndex, position, edge, printElementManager));
    }

    public Collection<PrintElementWithGraphElement> build() {
      result.addAll(nodes);
      return result;
    }
  }
}
