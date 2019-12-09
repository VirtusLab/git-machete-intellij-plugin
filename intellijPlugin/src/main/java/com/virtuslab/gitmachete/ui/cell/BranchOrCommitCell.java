package com.virtuslab.gitmachete.ui.cell;

import com.intellij.vcs.log.graph.PrintElement;
import com.virtuslab.gitmachete.graph.model.GraphElement;
import java.util.Collection;
import javax.annotation.Nonnull;
import lombok.Getter;

public class BranchOrCommitCell {
  @Getter @Nonnull private final GraphElement element;
  @Getter @Nonnull private final String text;
  @Getter @Nonnull private final Collection<? extends PrintElement> printElements;

  public BranchOrCommitCell(
      @Nonnull GraphElement graphElement,
      @Nonnull Collection<? extends PrintElement> printElements) {
    this.text = graphElement.getValue();
    this.printElements = printElements;
    this.element = graphElement;
  }

  @Override
  public String toString() {
    return text;
  }
}
