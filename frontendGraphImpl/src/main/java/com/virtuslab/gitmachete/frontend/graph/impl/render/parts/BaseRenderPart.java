package com.virtuslab.gitmachete.frontend.graph.impl.render.parts;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;
import com.virtuslab.gitmachete.frontend.graph.impl.render.GraphItemColorForGraphElementProvider;

@RequiredArgsConstructor
public abstract class BaseRenderPart implements IRenderPart {
  @Getter(onMethod_ = {@Override})
  protected final @NonNegative int rowIndex;

  @Getter(onMethod_ = {@Override})
  protected final @NonNegative int positionInRow;

  protected final IGraphElement graphElement;

  private final GraphItemColorForGraphElementProvider renderPartColorIdProvider;

  @Override
  public GraphItemColor getGraphItemColor() {
    return renderPartColorIdProvider.getGraphItemColor(graphElement);
  }
}
