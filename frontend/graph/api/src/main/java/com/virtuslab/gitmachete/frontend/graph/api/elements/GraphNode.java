
package com.virtuslab.gitmachete.frontend.graph.api.elements;

import io.vavr.NotImplementedError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

@RequiredArgsConstructor
public final class GraphNode implements IGraphElement {

  @Getter
  private final @NonNegative int nodeIndex;

  @Override
  public boolean isNode() {
    return true;
  }

  @Override
  public GraphNode asNode() {
    return this;
  }

  @Override
  public GraphEdge asEdge() {
    throw new NotImplementedError();
  }
}
