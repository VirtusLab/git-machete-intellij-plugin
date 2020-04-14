package com.virtuslab.gitmachete.frontend.graph.repository;

import java.util.Collection;
import java.util.Collections;

import com.intellij.util.SmartList;
import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.backend.api.NullRepository;
import com.virtuslab.gitmachete.frontend.graph.GraphElementManager;
import com.virtuslab.gitmachete.frontend.graph.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.print.PrintElementGeneratorImpl;

public class RepositoryGraph {
  @Getter
  @SuppressWarnings("ConstantName")
  private static final RepositoryGraph nullRepositoryGraph = new RepositoryGraphBuilder()
      .repository(NullRepository.getInstance()).build();

  private final List<IGraphElement> elements;
  private final List<List<Integer>> positionsOfVisibleEdges;

  private final PrintElementGeneratorImpl printElementGenerator;

  @SuppressWarnings({"nullness:argument.type.incompatible", "nullness:assignment.type.incompatible"})
  public RepositoryGraph(List<IGraphElement> elements, List<List<Integer>> positionsOfVisibleEdges) {
    this.elements = elements;
    this.positionsOfVisibleEdges = positionsOfVisibleEdges;

    GraphElementManager graphElementManager = new GraphElementManager(/* repositoryGraph */ this);
    printElementGenerator = new PrintElementGeneratorImpl(/* graph */ this, graphElementManager);
  }

  public Collection<? extends PrintElement> getPrintElements(int rowIndex) {
    return printElementGenerator.getPrintElements(rowIndex);
  }

  @SuppressWarnings("index:argument.type.incompatible")
  public IGraphElement getGraphElement(int rowIndex) {
    return elements.get(rowIndex);
  }

  @NonNegative
  public int nodesCount() {
    return elements.size();
  }

  public GraphNode getGraphNode(int nodeIndex) {
    return new GraphNode(nodeIndex);
  }

  public int getNodeId(int nodeIndex) {
    assert nodeIndex >= 0 && nodeIndex < nodesCount() : "Bad nodeIndex: " + nodeIndex;
    return nodeIndex;
  }

  /**
   * Adjacent edges are the edges that are visible in a row and directly connected to the node (branch/commit element)
   * of this row. See {@link RepositoryGraph#getVisibleEdgesWithPositions} for more details.
   *
   *  @return list of adjacent edges in a given node index
   */
  public java.util.List<GraphEdge> getAdjacentEdges(int nodeIndex, EdgeFilter filter) {
    if (filter == EdgeFilter.SPECIAL) {
      return Collections.emptyList();
    }

    java.util.List<GraphEdge> adjacentEdges = new SmartList<>();
    @SuppressWarnings("index:argument.type.incompatible")
    IGraphElement currentElement = elements.get(nodeIndex);

    if (filter.upNormal && nodeIndex > 0) {
      int upIndex = currentElement.getUpElementIndex();
      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    if (filter.downNormal && nodeIndex < elements.size() - 1) {
      Integer downElementIndex = currentElement.getDownElementIndex();
      if (downElementIndex != null) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, downElementIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }

  /**
   * Visible edges are the edges that are visible in a row but are NOT directly connected
   * to the node (branch/commit element) of this row. See {@link RepositoryGraph#getAdjacentEdges} for more details.
   *
   * @return list of visible edges in a given node index
   */
  @SuppressWarnings("index:argument.type.incompatible")
  public List<Tuple2<GraphEdge, Integer>> getVisibleEdgesWithPositions(int nodeIndex) {
    assert nodeIndex >= 0 && nodeIndex < nodesCount() : "Bad nodeIndex: " + nodeIndex;
    return positionsOfVisibleEdges.get(nodeIndex).map(pos -> {

      int downNodeIndex = nodeIndex + 1;
      int upNodeIndex = nodeIndex - 1;

      while (positionsOfVisibleEdges.get(downNodeIndex).contains(pos)) {
        downNodeIndex++;
      }

      while (positionsOfVisibleEdges.get(upNodeIndex).contains(pos)) {
        upNodeIndex--;
      }

      return Tuple.of(new GraphEdge(upNodeIndex, downNodeIndex, null, GraphEdgeType.USUAL), pos);
    }).collect(List.collector());
  }
}
