package com.virtuslab.gitmachete.frontend.graph.api.render.parts;

import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;

/**
 * Render parts ({@link IRenderPart}, {@link IEdgeRenderPart}, {@link INodeRenderPart}) represent RENDERED graph.
 * The position of unit parts (row of and position within graph table), orientation of edges are important here.
 * In this context the connections between graph elements does not matter (unlike {@link IGraphElement}s).
 * */
public interface IRenderPart {

  @NonNegative
  int getRowIndex();

  @NonNegative
  int getPositionInRow();

  int getColorId();

  boolean isNode();

  INodeRenderPart asNode();

  IEdgeRenderPart asEdge();
}
