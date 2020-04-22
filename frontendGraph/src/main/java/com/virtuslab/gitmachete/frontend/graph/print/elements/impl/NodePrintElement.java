
package com.virtuslab.gitmachete.frontend.graph.print.elements.impl;

import com.virtuslab.gitmachete.frontend.graph.print.elements.api.IEdgePrintElement;
import io.vavr.NotImplementedError;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.graph.GraphElementManager;
import com.virtuslab.gitmachete.frontend.graph.api.GraphNode;
import com.virtuslab.gitmachete.frontend.graph.print.elements.api.INodePrintElement;

public class NodePrintElement extends PrintElementWithGraphElement implements INodePrintElement {

  public NodePrintElement(@NonNegative int rowIndex,
      @NonNegative int positionInRow,
      GraphNode graphNode,
      GraphElementManager graphElementManager) {
    super(rowIndex, positionInRow, graphNode, graphElementManager);
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
