
package com.virtuslab.gitmachete.frontend.graph.impl.render.parts;

import io.vavr.NotImplementedError;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.GraphNode;
import com.virtuslab.gitmachete.frontend.graph.api.render.IRenderPartColorIdProvider;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IEdgeRenderPart;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.INodeRenderPart;

public final class NodeRenderPart extends BaseRenderPart implements INodeRenderPart {

  public NodeRenderPart(@NonNegative int rowIndex,
      @NonNegative int positionInRow,
      GraphNode graphNode,
      IRenderPartColorIdProvider renderPartColorIdProvider) {
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
