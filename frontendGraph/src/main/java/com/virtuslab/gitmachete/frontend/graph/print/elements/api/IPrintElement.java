package com.virtuslab.gitmachete.frontend.graph.print.elements.api;

import org.checkerframework.checker.index.qual.NonNegative;

public interface IPrintElement {

  @NonNegative
  int getRowIndex();

  @NonNegative
  int getPositionInRow();

  int getColorId();

  boolean isNode();

  INodePrintElement asNode();

  IEdgePrintElement asEdge();
}
