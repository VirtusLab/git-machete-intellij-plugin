
package com.virtuslab.gitmachete.frontend.graph.impl.render.parts;

import io.vavr.NotImplementedError;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.GraphEdge;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IEdgeRenderPart;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.INodeRenderPart;
import com.virtuslab.gitmachete.frontend.graph.impl.render.GraphItemColorForGraphElementProvider;

@Getter
public final class EdgeRenderPart extends BaseRenderPart implements IEdgeRenderPart {

  private final Type type;

  public EdgeRenderPart(
      @NonNegative int rowIndex,
      @NonNegative int positionInRow,
      Type type,
      GraphEdge graphEdge,
      GraphItemColorForGraphElementProvider renderPartColorIdProvider) {
    super(rowIndex, positionInRow, graphEdge, renderPartColorIdProvider);
    this.type = type;
  }

  @Override
  public boolean isNode() {
    return false;
  }

  @Override
  public INodeRenderPart asNode() {
    throw new NotImplementedError();
  }

  @Override
  public IEdgeRenderPart asEdge() {
    return this;
  }
}
