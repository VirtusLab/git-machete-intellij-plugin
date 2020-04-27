
package com.virtuslab.gitmachete.frontend.graph.api.elements;

import io.vavr.NotImplementedError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

@Getter
@RequiredArgsConstructor
public final class GraphNode implements IGraphElement {

  @NonNegative
  private final int nodeIndex;

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
