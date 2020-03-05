package com.virtuslab.gitmachete.frontend.ui.cell;

import java.util.Collection;

import javax.annotation.Nonnull;

import lombok.Data;

import com.intellij.vcs.log.graph.PrintElement;

import com.virtuslab.gitmachete.frontend.graph.model.IGraphElement;

@Data
public final class BranchOrCommitCell {
  @Nonnull
  private final IGraphElement element;
  @Nonnull
  private final String text;
  @Nonnull
  private final Collection<? extends PrintElement> printElements;

  public BranchOrCommitCell(
      @Nonnull IGraphElement graphElement,
      @Nonnull Collection<? extends PrintElement> printElements) {
    this.text = graphElement.getValue();
    this.printElements = printElements;
    this.element = graphElement;
  }
}
