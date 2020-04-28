
package com.virtuslab.gitmachete.frontend.graph.api.elements;

/**
 * Graph elements ({@link IGraphElement}, {@link GraphEdge}, {@link GraphNode}) represent the LOGICAL graph structure.
 * In this scope there are only graph nodes and edges connecting them.
 * The actual position of elements (in graph table or relative to each other) does not count
 * here (unlike with {@link com.virtuslab.gitmachete.frontend.graph.api.print.elements.IPrintElement}).
 * */
public interface IGraphElement {

  boolean isNode();

  GraphNode asNode();

  GraphEdge asEdge();
}
