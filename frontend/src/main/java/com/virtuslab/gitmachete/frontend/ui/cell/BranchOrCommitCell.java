package com.virtuslab.gitmachete.frontend.ui.cell;

import java.util.Collection;

import com.intellij.vcs.log.graph.PrintElement;
import lombok.Data;

import com.virtuslab.gitmachete.frontend.graph.model.IGraphElement;

@Data
public final class BranchOrCommitCell {
  private final IGraphElement element;
  private final String text;
  private final Collection<? extends PrintElement> printElements;

  public BranchOrCommitCell(
      IGraphElement graphElement,
      Collection<? extends PrintElement> printElements) {
    this.text = graphElement.getValue();
    this.printElements = printElements;
    this.element = graphElement;
  }
}
