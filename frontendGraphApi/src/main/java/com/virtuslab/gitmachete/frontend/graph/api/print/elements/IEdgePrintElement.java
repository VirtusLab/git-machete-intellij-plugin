package com.virtuslab.gitmachete.frontend.graph.api.print.elements;

public interface IEdgePrintElement extends IPrintElement {

  Type getType();

  enum Type {
    UP, DOWN, RIGHT
  }
}
