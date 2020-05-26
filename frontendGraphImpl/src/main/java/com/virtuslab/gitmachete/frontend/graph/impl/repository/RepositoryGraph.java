package com.virtuslab.gitmachete.frontend.graph.impl.repository;

import com.intellij.util.SmartList;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;

import com.virtuslab.gitmachete.frontend.graph.api.elements.GraphEdge;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.render.IRenderPartGenerator;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.impl.render.RenderPartGenerator;

public class RepositoryGraph implements IRepositoryGraph {

  private final List<IGraphItem> items;
  private final List<List<Integer>> positionsOfVisibleEdges;
  @NotOnlyInitialized
  private final IRenderPartGenerator renderPartGenerator;

  public RepositoryGraph(List<IGraphItem> items, List<List<Integer>> positionsOfVisibleEdges) {
    this.items = items;
    this.positionsOfVisibleEdges = positionsOfVisibleEdges;
    this.renderPartGenerator = new RenderPartGenerator(/* repositoryGraph */ this);
  }

  /**
   * Adjacent edges are the edges that are visible in a row and directly connected to the node
   * (representing branch/commit item) of this row. See {@link RepositoryGraph#getVisibleEdgesWithPositions} for more details.
   *
   * @param itemIndex item index
   * @return list of adjacent edges in a given item index
   */
  public List<GraphEdge> getAdjacentEdges(@NonNegative int itemIndex) {
    java.util.List<GraphEdge> adjacentEdges = new SmartList<>();
    @SuppressWarnings("upperbound:argument.type.incompatible") IGraphItem currentItem = items.get(itemIndex);

    if (itemIndex > 0) {
      int upNodeIndex = currentItem.getPrevSiblingItemIndex();
      if (upNodeIndex >= 0) {
        adjacentEdges.add(GraphEdge.createEdge(/* nodeIndex1 */ itemIndex, /* nodeIndex2 */ upNodeIndex));
      }
    }

    if (itemIndex < items.size() - 1) {
      Integer nextSiblingItemIndex = currentItem.getNextSiblingItemIndex();
      if (nextSiblingItemIndex != null) {
        adjacentEdges.add(GraphEdge.createEdge(/* nodeIndex1 */ itemIndex, /* nodeIndex2 */ nextSiblingItemIndex));
      }
    }

    return List.ofAll(adjacentEdges);
  }

  @SuppressWarnings("upperbound:argument.type.incompatible")
  public IGraphItem getGraphItem(@NonNegative int itemIndex) {
    return items.get(itemIndex);
  }

  public @NonNegative int getNodesCount() {
    return items.size();
  }

  public List<? extends IRenderPart> getRenderParts(@NonNegative int itemIndex) {
    return renderPartGenerator.getRenderParts(itemIndex);
  }

  /**
   * Visible edges are the edges that are visible in a row but are NOT directly connected to the node
   * (representing branch/commit item) of this row. See {@link RepositoryGraph#getAdjacentEdges} for more details.
   *
   * @param itemIndex item index
   * @return list of visible edges in a given item index
   */
  public List<Tuple2<GraphEdge, @NonNegative Integer>> getVisibleEdgesWithPositions(@NonNegative int itemIndex) {
    assert itemIndex < positionsOfVisibleEdges.size() : "Bad itemIndex: " + itemIndex;

    return positionsOfVisibleEdges.get(itemIndex).map(pos -> {

      int upNodeIndex = itemIndex - 1;
      int downNodeIndex = itemIndex + 1;

      while (downNodeIndex < positionsOfVisibleEdges.size() && positionsOfVisibleEdges.get(downNodeIndex).contains(pos)) {
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
