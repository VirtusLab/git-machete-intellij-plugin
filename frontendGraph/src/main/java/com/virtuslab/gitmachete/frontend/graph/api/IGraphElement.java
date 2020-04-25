
package com.virtuslab.gitmachete.frontend.graph.api;

public interface IGraphElement {

  boolean isNode();

  GraphNode asNode();

  GraphEdge asEdge();
}
