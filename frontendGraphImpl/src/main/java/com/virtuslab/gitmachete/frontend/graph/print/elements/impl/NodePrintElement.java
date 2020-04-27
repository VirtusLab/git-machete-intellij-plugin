
package com.virtuslab.gitmachete.frontend.graph.print.elements.impl;

import io.vavr.NotImplementedError;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.GraphNode;
import com.virtuslab.gitmachete.frontend.graph.print.elements.PrintElementColorManager;
import com.virtuslab.gitmachete.frontend.graph.print.elements.api.IEdgePrintElement;
import com.virtuslab.gitmachete.frontend.graph.print.elements.api.INodePrintElement;

public final class NodePrintElement extends PrintElementWithGraphElement implements INodePrintElement {

  public NodePrintElement(@NonNegative int rowIndex,
      @NonNegative int positionInRow,
      GraphNode graphNode,
      PrintElementColorManager printElementColorManager) {
    super(rowIndex, positionInRow, graphNode, printElementColorManager);
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
