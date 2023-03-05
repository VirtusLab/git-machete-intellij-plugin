
package com.virtuslab.gitmachete.frontend.graph.api.elements;

import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.RequiresQualifier;

import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;
import com.virtuslab.qual.subtyping.gitmachete.frontend.graph.api.elements.ConfirmedGraphEdge;
import com.virtuslab.qual.subtyping.gitmachete.frontend.graph.api.elements.ConfirmedGraphNode;

/**
 * Graph elements ({@link IGraphElement}, {@link GraphEdge}, {@link GraphNode}) represent the LOGICAL graph structure.
 * In this scope there are only graph nodes and edges connecting them.
 * The actual position of elements (in graph table or relative to each other) does not count
 * here (unlike with {@link IRenderPart}).
 * */
public interface IGraphElement {
  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedGraphNode.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedGraphEdge.class)
  boolean isNode();

  @EnsuresQualifierIf(expression = "this", result = true, qualifier = ConfirmedGraphEdge.class)
  @EnsuresQualifierIf(expression = "this", result = false, qualifier = ConfirmedGraphNode.class)
  default boolean isEdge() {
    return !isNode();
  }

  @RequiresQualifier(expression = "this", qualifier = ConfirmedGraphNode.class)
  GraphNode asNode();

  @RequiresQualifier(expression = "this", qualifier = ConfirmedGraphEdge.class)
  GraphEdge asEdge();
}
