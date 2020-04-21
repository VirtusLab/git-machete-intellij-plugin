package com.virtuslab.gitmachete.frontend.graph.print;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.intellij.util.SmartList;
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
import com.virtuslab.gitmachete.frontend.graph.nodes.BranchNode;
import com.virtuslab.gitmachete.frontend.graph.nodes.IGraphNode;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;

@RequiredArgsConstructor
public final class PrintElementGeneratorImpl implements PrintElementGenerator {
  private final RepositoryGraph repositoryGraph;
  private final GraphElementManager graphElementManager;

  @Override
  public Collection<PrintElementWithGraphElement> getPrintElements(int rowIndex) {
    assert rowIndex >= 0;
    PrintElementBuilder builder = new PrintElementBuilder(rowIndex);
    collectElements(rowIndex, builder);
    return builder.build();
  }

  private void collectElements(@NonNegative int rowIndex, PrintElementBuilder builder) {
    IGraphNode graphNode = repositoryGraph.getGraphNode(rowIndex);
    int position = graphNode.getIndentLevel();

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
    if (graphNode.isBranch() && !((BranchNode) graphNode).getBranch().isRootBranch()) {
      builder.consumeRightEdge(new GraphEdge(rowIndex, rowIndex, /* targetId */ null, GraphEdgeType.USUAL), position);
      nodeAndItsDownEdgePos++;
    }

    builder.consumeNode(new GraphNode(rowIndex), nodeAndItsDownEdgePos);
    if (graphNode.hasChildNode()) {
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
      nodes.add(new SimplePrintElementImpl(rowIndex, position, node, graphElementManager));
    }

    public void consumeDownEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(
          new EdgePrintElementImpl(rowIndex, position, position, Type.DOWN, edge, /* hasArrow */ false,
              graphElementManager));
    }

    public void consumeUpEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(
          new EdgePrintElementImpl(rowIndex, position, position, Type.UP, edge, /* hasArrow */ false,
              graphElementManager));
    }

    public void consumeRightEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(new RightEdgePrintElement(rowIndex, position, edge, graphElementManager));
    }

    public Collection<PrintElementWithGraphElement> build() {
      List<PrintElementWithGraphElement> result = new ArrayList<>(edges);
      result.addAll(nodes);
      return result;
    }
  }
}
