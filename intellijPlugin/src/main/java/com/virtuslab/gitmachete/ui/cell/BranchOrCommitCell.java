package com.virtuslab.gitmachete.ui.cell;

import com.intellij.vcs.log.graph.PrintElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import java.util.Collection;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public final class BranchOrCommitCell {
  @Getter @Nonnull private final IGraphElement element;
  @Getter @Nonnull private final String text;
  @Getter @Nonnull private final Collection<? extends PrintElement> printElements;

  public BranchOrCommitCell(
      @Nonnull IGraphElement graphElement,
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
