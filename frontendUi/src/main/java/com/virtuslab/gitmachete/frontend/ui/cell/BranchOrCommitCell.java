package com.virtuslab.gitmachete.frontend.ui.cell;

import java.util.Collection;

import com.intellij.vcs.log.graph.PrintElement;
import lombok.Data;

import com.virtuslab.gitmachete.frontend.graph.nodes.IGraphNode;

@Data
public final class BranchOrCommitCell {
  private final IGraphNode graphNode;
  private final String text;
  private final Collection<? extends PrintElement> printElements;

  public BranchOrCommitCell(
      IGraphNode graphNode,
      Collection<? extends PrintElement> printElements) {
    this.text = graphNode.getValue();
    this.printElements = printElements;
    this.graphNode = graphNode;
  }
}
