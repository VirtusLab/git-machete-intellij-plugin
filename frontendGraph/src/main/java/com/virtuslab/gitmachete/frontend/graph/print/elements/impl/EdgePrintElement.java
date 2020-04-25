
package com.virtuslab.gitmachete.frontend.graph.print.elements.impl;

import io.vavr.NotImplementedError;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.GraphElementManager;
import com.virtuslab.gitmachete.frontend.graph.api.GraphEdge;
import com.virtuslab.gitmachete.frontend.graph.print.elements.api.IEdgePrintElement;
import com.virtuslab.gitmachete.frontend.graph.print.elements.api.INodePrintElement;

@Getter
public final class EdgePrintElement extends PrintElementWithGraphElement implements IEdgePrintElement {

  private final Type type;

  public EdgePrintElement(@NonNegative int rowIndex,
      @NonNegative int positionInRow,
      Type type,
      GraphEdge graphEdge,
      GraphElementManager graphElementManager) {
    super(rowIndex, positionInRow, graphEdge, graphElementManager);
    this.type = type;
  }

  @Override
  public boolean isNode() {
    return false;
  }

  @Override
  public INodePrintElement asNode() {
    throw new NotImplementedError();
  }

  @Override
  public IEdgePrintElement asEdge() {
    return this;
  }
}
