
package com.virtuslab.gitmachete.frontend.graph.impl.print.elements;

import io.vavr.NotImplementedError;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.GraphEdge;
import com.virtuslab.gitmachete.frontend.graph.api.print.IPrintElementColorIdProvider;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IEdgePrintElement;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.INodePrintElement;

@Getter
public final class EdgePrintElement extends PrintElementWithGraphElement implements IEdgePrintElement {

  private final Type type;

  public EdgePrintElement(@NonNegative int rowIndex,
      @NonNegative int positionInRow,
      Type type,
      GraphEdge graphEdge,
      IPrintElementColorIdProvider printElementColorIdProvider) {
    super(rowIndex, positionInRow, graphEdge, printElementColorIdProvider);
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
