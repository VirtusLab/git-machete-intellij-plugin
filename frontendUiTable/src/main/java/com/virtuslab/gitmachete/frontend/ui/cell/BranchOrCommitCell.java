package com.virtuslab.gitmachete.frontend.ui.cell;

import java.util.Collection;

import lombok.Data;

import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IPrintElement;

@Data
public final class BranchOrCommitCell {
  private final IGraphItem graphItem;
  private final String text;
  private final Collection<? extends IPrintElement> printElements;

  public BranchOrCommitCell(
      IGraphItem graphItem,
      Collection<? extends IPrintElement> printElements) {
    this.text = graphItem.getValue();
    this.printElements = printElements;
    this.graphItem = graphItem;
  }
}
