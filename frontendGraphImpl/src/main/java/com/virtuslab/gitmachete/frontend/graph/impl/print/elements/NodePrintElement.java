
package com.virtuslab.gitmachete.frontend.graph.impl.print.elements;

import io.vavr.NotImplementedError;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.GraphNode;
import com.virtuslab.gitmachete.frontend.graph.api.print.IPrintElementColorIdProvider;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IEdgePrintElement;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.INodePrintElement;

public final class NodePrintElement extends PrintElementWithGraphElement implements INodePrintElement {

  public NodePrintElement(@NonNegative int rowIndex,
      @NonNegative int positionInRow,
      GraphNode graphNode,
      IPrintElementColorIdProvider printElementColorIdProvider) {
    super(rowIndex, positionInRow, graphNode, printElementColorIdProvider);
  }

  @Override
  public boolean isNode() {
    return true;
  }

  @Override
  public INodePrintElement asNode() {
    return this;
  }

  @Override
  public IEdgePrintElement asEdge() {
    throw new NotImplementedError();
  }
}
