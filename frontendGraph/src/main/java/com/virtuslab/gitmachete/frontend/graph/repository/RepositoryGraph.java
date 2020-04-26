package com.virtuslab.gitmachete.frontend.graph.repository;

import java.util.Collection;

import com.intellij.util.SmartList;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.backend.api.NullRepository;
import com.virtuslab.gitmachete.frontend.graph.GraphElementManager;
import com.virtuslab.gitmachete.frontend.graph.api.GraphEdge;
import com.virtuslab.gitmachete.frontend.graph.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.print.PrintElementGenerator;
import com.virtuslab.gitmachete.frontend.graph.print.elements.api.IPrintElement;

public class RepositoryGraph {
  @Getter
  @SuppressWarnings("ConstantName")
  private static final RepositoryGraph nullRepositoryGraph = new RepositoryGraphBuilder()
      .repository(NullRepository.getInstance()).build();

  private final List<IGraphItem> items;
  private final List<List<Integer>> positionsOfVisibleEdges;

  private final PrintElementGenerator printElementGenerator;

  @SuppressWarnings({"nullness:argument.type.incompatible", "nullness:assignment.type.incompatible"})
  public RepositoryGraph(List<IGraphItem> items, List<List<Integer>> positionsOfVisibleEdges) {
    this.items = items;
    this.positionsOfVisibleEdges = positionsOfVisibleEdges;

    GraphElementManager graphElementManager = new GraphElementManager(/* repositoryGraph */ this);
    printElementGenerator = new PrintElementGenerator(/* graph */ this, graphElementManager);
  }

  public Collection<? extends IPrintElement> getPrintElements(@NonNegative int nodeIndex) {
    return printElementGenerator.getPrintElements(nodeIndex);
  }

  @SuppressWarnings("upperbound:argument.type.incompatible")
  public IGraphItem getGraphItem(@NonNegative int itemIndex) {
    return items.get(itemIndex);
  }

  @NonNegative
  public int nodesCount() {
    return items.size();
  }

  /**
   * Adjacent edges are the edges that are visible in a row and directly connected to the node (branch/commit node)
   * of this row. See {@link RepositoryGraph#getVisibleEdgesWithPositions} for more details.
   *
   * @param nodeIndex node index
   * @return list of adjacent edges in a given node index
   */
  public java.util.List<GraphEdge> getAdjacentEdges(@NonNegative int nodeIndex) {
    java.util.List<GraphEdge> adjacentEdges = new SmartList<>();
    @SuppressWarnings("upperbound:argument.type.incompatible")
    IGraphItem currentItem = items.get(nodeIndex);

    if (nodeIndex > 0) {
      int upIndex = currentItem.getPrevSiblingItemIndex();
      if (upIndex >= 0) {
        adjacentEdges.add(GraphEdge.createEdge(nodeIndex, upIndex));
      }
    }

    if (nodeIndex < items.size() - 1) {
      Integer nextSiblingNodeIndex = currentItem.getNextSiblingItemIndex();
      if (nextSiblingNodeIndex != null) {
        adjacentEdges.add(GraphEdge.createEdge(nodeIndex, nextSiblingNodeIndex));
      }
    }

    return adjacentEdges;
  }

  /**
   * Visible edges are the edges that are visible in a row but are NOT directly connected to the node
   * (representing branch/commit item) of this row. See {@link RepositoryGraph#getAdjacentEdges} for more details.
   *
   * @param nodeIndex node index
   * @return list of visible edges in a given node index
   */
  public List<Tuple2<GraphEdge, @NonNegative Integer>> getVisibleEdgesWithPositions(@NonNegative int nodeIndex) {
    assert nodeIndex < positionsOfVisibleEdges.size() : "Bad nodeIndex: " + nodeIndex;

    return positionsOfVisibleEdges.get(nodeIndex).map(pos -> {

      int upNodeIndex = nodeIndex - 1;
      int downNodeIndex = nodeIndex + 1;

      while (downNodeIndex < positionsOfVisibleEdges.size()
          && positionsOfVisibleEdges.get(downNodeIndex).contains(pos)) {
        downNodeIndex++;
      }

      while (upNodeIndex >= 0 && positionsOfVisibleEdges.get(upNodeIndex).contains(pos)) {
        upNodeIndex--;
      }

      // We can assume the following since we know that the first node (a root branch)
      // AND the last node (some branch, possible indented child) has no visible edges in their rows.
      // (The first condition is obvious. The second can be easily proved by contradiction.
      // Suppose that the last node, at index n has a visible edge. Any visible edge has some (branch) node
      // that it leads to, hence there must exist some node at index k > n being a target to the visible edge.
      // But n is the index of the last node. Contradiction. )
      assert upNodeIndex >= 0
          && downNodeIndex < positionsOfVisibleEdges.size() : "upNodeIndex or downNodeIndex has wrong value";

      return Tuple.of(new GraphEdge(upNodeIndex, downNodeIndex), pos);
    }).collect(List.collector());
  }
}
