package com.virtuslab.gitmachete.frontend.graph.api.print.elements;

import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;

/**
 * Print elements ({@link IPrintElement}, {@link IEdgePrintElement}, {@link INodePrintElement}) represent PRINTED graph.
 * The position of elements (row of and position within graph table), orientation of edges are important here.
 * In this context the connections between graph elements does not matter (unlike {@link IGraphElement}s).
 * */
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
