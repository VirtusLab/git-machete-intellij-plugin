
package com.virtuslab.gitmachete.frontend.graph.impl.render.parts;

import io.vavr.NotImplementedError;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.GraphNode;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IEdgeRenderPart;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.INodeRenderPart;
import com.virtuslab.gitmachete.frontend.graph.impl.render.GraphItemColorForGraphElementProvider;

public final class NodeRenderPart extends BaseRenderPart implements INodeRenderPart {

  public NodeRenderPart(
      @NonNegative int rowIndex,
      @NonNegative int positionInRow,
      GraphNode graphNode,
      GraphItemColorForGraphElementProvider renderPartColorIdProvider) {
    super(rowIndex, positionInRow, graphNode, renderPartColorIdProvider);
  }

  @Override
  public boolean isNode() {
    return true;
  }

  @Override
  public INodeRenderPart asNode() {
    return this;
  }

  @Override
  public IEdgeRenderPart asEdge() {
    throw new NotImplementedError();
  }
}
