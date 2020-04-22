package com.virtuslab.gitmachete.frontend.graph.print.elements.api;

public interface IEdgePrintElement extends IPrintElement {

  Type getType();

  enum Type {
    UP, DOWN, RIGHT
  }
}
