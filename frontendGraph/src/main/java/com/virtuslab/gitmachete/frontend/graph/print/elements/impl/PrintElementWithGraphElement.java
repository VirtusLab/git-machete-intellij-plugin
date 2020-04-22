package com.virtuslab.gitmachete.frontend.graph.print.elements.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.GraphElementManager;
import com.virtuslab.gitmachete.frontend.graph.api.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.print.elements.api.IPrintElement;

@Getter
@RequiredArgsConstructor
public abstract class PrintElementWithGraphElement implements IPrintElement {
  @NonNegative
  protected final int rowIndex;
  @NonNegative
  protected final int positionInRow;
  protected final IGraphElement graphElement;
  protected final GraphElementManager printElementManager;

  @Override
  public int getColorId() {
    return printElementManager.getColorId(graphElement);
  }
}
