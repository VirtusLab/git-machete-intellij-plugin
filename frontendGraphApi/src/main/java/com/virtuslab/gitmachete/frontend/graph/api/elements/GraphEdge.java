package com.virtuslab.gitmachete.frontend.graph.api.elements;

import io.vavr.NotImplementedError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

@Getter
@RequiredArgsConstructor
public final class GraphEdge implements IGraphElement {
  public static GraphEdge createEdge(@NonNegative int nodeIndex1, @NonNegative int nodeIndex2) {
    return new GraphEdge(Math.min(nodeIndex1, nodeIndex2), Math.max(nodeIndex1, nodeIndex2));
  }

  @NonNegative
  private final int upNodeIndex;

  @NonNegative
  private final int downNodeIndex;

  @Override
  public boolean isNode() {
    return false;
  }

  @Override
  public GraphNode asNode() {
    throw new NotImplementedError();
  }

  @Override
  public GraphEdge asEdge() {
    return this;
  }
}
