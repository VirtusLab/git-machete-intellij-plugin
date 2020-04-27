package com.virtuslab.gitmachete.frontend.graph.impl.print.elements;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IPrintElement;
import com.virtuslab.gitmachete.frontend.graph.impl.print.PrintElementColorManager;

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
