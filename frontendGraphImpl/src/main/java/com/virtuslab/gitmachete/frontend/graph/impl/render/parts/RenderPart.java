package com.virtuslab.gitmachete.frontend.graph.impl.render.parts;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.api.render.IRenderPartColorIdProvider;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;

@Getter
@RequiredArgsConstructor
public abstract class RenderPart implements IRenderPart {
  @NonNegative
  protected final int rowIndex;
  @NonNegative
  protected final int positionInRow;
  protected final IGraphElement graphElement;

  private final IRenderPartColorIdProvider renderPartColorIdProvider;

  @Override
  public int getColorId() {
    return renderPartColorIdProvider.getColorId(graphElement);
  }
}
