package com.virtuslab.gitmachete.frontend.graph.api.render.parts;

public interface IEdgeRenderPart extends IRenderPart {

  Type getType();

  enum Type {
    UP, DOWN, RIGHT
  }
}
