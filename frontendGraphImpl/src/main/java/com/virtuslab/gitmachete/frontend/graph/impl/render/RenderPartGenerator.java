package com.virtuslab.gitmachete.frontend.graph.impl.render;

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
import com.virtuslab.gitmachete.frontend.graph.api.render.IRenderPartColorIdProvider;
import com.virtuslab.gitmachete.frontend.graph.api.render.IRenderPartGenerator;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IEdgeRenderPart;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.impl.render.parts.BaseRenderPart;
import com.virtuslab.gitmachete.frontend.graph.impl.render.parts.EdgeRenderPart;
import com.virtuslab.gitmachete.frontend.graph.impl.render.parts.NodeRenderPart;

public final class RenderPartGenerator implements IRenderPartGenerator {
  @NotOnlyInitialized
  private final IRepositoryGraph repositoryGraph;
  @NotOnlyInitialized
  private final IRenderPartColorIdProvider renderPartColorIdProvider;

  public RenderPartGenerator(@UnderInitialization IRepositoryGraph repositoryGraph) {
    this.repositoryGraph = repositoryGraph;
    this.renderPartColorIdProvider = new RenderPartColorIdProvider(repositoryGraph);
  }

  @Override
  public List<BaseRenderPart> getRenderParts(@NonNegative int rowIndex) {
    RenderPartBuilder builder = new RenderPartBuilder(rowIndex);
    collectParts(rowIndex, builder);
    return builder.build();
  }

  private void collectParts(@NonNegative int rowIndex, RenderPartBuilder builder) {
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
    if (graphItem.isBranchItem() && graphItem.asBranchItem().getBranch().isNonRootBranch()) {
      builder.consumeRightEdge(new GraphEdge(rowIndex, rowIndex), position);
      nodeAndItsDownEdgePos++;
    }

    builder.consumeNode(new GraphNode(rowIndex), nodeAndItsDownEdgePos);
    if (graphItem.hasChildItem()) {
      builder.consumeDownEdge(new GraphEdge(rowIndex, rowIndex + 1), nodeAndItsDownEdgePos);
    }
  }

  @RequiredArgsConstructor
  private final class RenderPartBuilder {
    private final java.util.List<BaseRenderPart> edges = new ArrayList<>();
    private final java.util.List<BaseRenderPart> nodes = new SmartList<>();
    private final @NonNegative int rowIndex;

    public void consumeNode(GraphNode node, @NonNegative int position) {
      nodes.add(new NodeRenderPart(rowIndex, position, node, renderPartColorIdProvider));
    }

    public void consumeDownEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(
          new EdgeRenderPart(rowIndex, position, IEdgeRenderPart.Type.DOWN, edge, renderPartColorIdProvider));
    }

    public void consumeUpEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(new EdgeRenderPart(rowIndex, position, IEdgeRenderPart.Type.UP, edge, renderPartColorIdProvider));
    }

    public void consumeRightEdge(GraphEdge edge, @NonNegative int position) {
      edges.add(
          new EdgeRenderPart(rowIndex, position, IEdgeRenderPart.Type.RIGHT, edge, renderPartColorIdProvider));
    }

    public List<BaseRenderPart> build() {
      return List.ofAll(edges).appendAll(nodes);
    }
  }
}
