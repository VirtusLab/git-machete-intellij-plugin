package com.virtuslab.gitmachete.frontend.graph.print.elements.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.print.elements.PrintElementColorManager;
import com.virtuslab.gitmachete.frontend.graph.print.elements.api.IPrintElement;

@Getter
@RequiredArgsConstructor
public abstract class PrintElementWithGraphElement implements IPrintElement {
  @NonNegative
  protected final int rowIndex;
  @NonNegative
  protected final int positionInRow;
  protected final IGraphElement graphElement;

  private final PrintElementColorManager printElementColorManager;

  @Override
  public int getColorId() {
    return printElementColorManager.getColorId(graphElement);
  }
}
