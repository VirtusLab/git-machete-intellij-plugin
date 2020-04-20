package com.virtuslab.gitmachete.frontend.graph.repository;

import java.util.Collection;
import java.util.Collections;

import com.intellij.util.SmartList;
import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.backend.api.NullRepository;
import com.virtuslab.gitmachete.frontend.graph.GraphElementManager;
import com.virtuslab.gitmachete.frontend.graph.nodes.IGraphNode;
import com.virtuslab.gitmachete.frontend.graph.print.PrintElementGeneratorImpl;

public class RepositoryGraph {
  @Getter
  @SuppressWarnings("ConstantName")
  private static final RepositoryGraph nullRepositoryGraph = new RepositoryGraphBuilder()
      .repository(NullRepository.getInstance()).build();

  private final List<IGraphNode> nodes;
  private final List<List<Integer>> positionsOfVisibleEdges;

  private final PrintElementGeneratorImpl printElementGenerator;

  @SuppressWarnings({"nullness:argument.type.incompatible", "nullness:assignment.type.incompatible"})
  public RepositoryGraph(List<IGraphNode> nodes, List<List<Integer>> positionsOfVisibleEdges) {
    this.nodes = nodes;
    this.positionsOfVisibleEdges = positionsOfVisibleEdges;

    GraphElementManager graphElementManager = new GraphElementManager(/* repositoryGraph */ this);
    printElementGenerator = new PrintElementGeneratorImpl(/* graph */ this, graphElementManager);
  }

  public Collection<? extends PrintElement> getPrintElements(@NonNegative int rowIndex) {
    return printElementGenerator.getPrintElements(rowIndex);
  }

  @SuppressWarnings("index:argument.type.incompatible")
  public IGraphNode getGraphNode(@NonNegative int rowIndex) {
    return nodes.get(rowIndex);
  }

  @NonNegative
  public int nodesCount() {
    return nodes.size();
  }

  public int getNodeId(int nodeIndex) {
    assert nodeIndex >= 0 && nodeIndex < nodesCount() : "Bad nodeIndex: " + nodeIndex;
    return nodeIndex;
  }

  /**
   * Adjacent edges are the edges that are visible in a row and directly connected to the node (branch/commit node)
   * of this row. See {@link RepositoryGraph#getVisibleEdgesWithPositions} for more details.
   *
   *  @return list of adjacent edges in a given node index
   */
  public java.util.List<GraphEdge> getAdjacentEdges(@NonNegative int nodeIndex, EdgeFilter filter) {
    if (filter == EdgeFilter.SPECIAL) {
      return Collections.emptyList();
    }

    java.util.List<GraphEdge> adjacentEdges = new SmartList<>();
    @SuppressWarnings("index:argument.type.incompatible")
    IGraphNode currentNode = nodes.get(nodeIndex);

    if (filter.upNormal && nodeIndex > 0) {
      int upIndex = currentNode.getPrevSiblingNodeIndex();
      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, upIndex, GraphEdgeType.USUAL));
      }
    }

    if (filter.downNormal && nodeIndex < nodes.size() - 1) {
      Integer nextSiblingNodeIndex = currentNode.getNextSiblingNodeIndex();
      if (nextSiblingNodeIndex != null) {
        adjacentEdges.add(GraphEdge.createNormalEdge(nodeIndex, nextSiblingNodeIndex, GraphEdgeType.USUAL));
      }
    }

    return adjacentEdges;
  }

  /**
   * Visible edges are the edges that are visible in a row but are NOT directly connected
   * to the node (branch/commit node) of this row. See {@link RepositoryGraph#getAdjacentEdges} for more details.
   *
   * @return list of visible edges in a given node index
   */
  @SuppressWarnings("index:argument.type.incompatible")
  public List<Tuple2<GraphEdge, @NonNegative Integer>> getVisibleEdgesWithPositions(@NonNegative int nodeIndex) {
    assert nodeIndex < nodesCount() : "Bad nodeIndex: " + nodeIndex;
    return positionsOfVisibleEdges.get(nodeIndex).map(pos -> {

      int upNodeIndex = nodeIndex - 1;
      int downNodeIndex = nodeIndex + 1;

      // We can assume the following since we know that the first node (a root branch)
      // AND the last node (some branch, possible indented child) has no visible edges in their rows.
      // (The first condition is obvious. The second can be easily proved by contradiction.
      // Suppose that the last node, at index n has a visible edge. Any visible edge has some (branch) node
      // that it leads to, hence there must exists some node at index k > n being a target to the visible edge.
      // But n is the index of the last node. Contradiction. )
      assert upNodeIndex >= 0 && downNodeIndex < nodesCount();

      while (positionsOfVisibleEdges.get(downNodeIndex).contains(pos)) {
        downNodeIndex++;
      }

      while (positionsOfVisibleEdges.get(upNodeIndex).contains(pos)) {
        upNodeIndex--;
      }

      return Tuple.of(new GraphEdge(upNodeIndex, downNodeIndex, /* targetId */ null, GraphEdgeType.USUAL), pos);
    }).collect(List.collector());
  }
}
