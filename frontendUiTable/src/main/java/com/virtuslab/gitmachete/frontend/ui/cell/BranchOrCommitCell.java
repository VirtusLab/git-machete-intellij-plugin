package com.virtuslab.gitmachete.frontend.ui.cell;

import java.util.Collection;

import com.intellij.vcs.log.graph.PrintElement;
import lombok.Data;

import com.virtuslab.gitmachete.frontend.graph.items.IGraphItem;

@Data
public final class BranchOrCommitCell {
  private final IGraphItem graphItem;
  private final String text;
  private final Collection<? extends PrintElement> printElements;

  public BranchOrCommitCell(
      IGraphItem graphItem,
      Collection<? extends PrintElement> printElements) {
    this.text = graphItem.getValue();
    this.printElements = printElements;
    this.graphItem = graphItem;
  }
}
