package com.virtuslab.gitmachete.ui.cell;

import com.intellij.vcs.log.graph.PrintElement;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import java.util.Collection;
import javax.annotation.Nonnull;
import lombok.Getter;

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

  @Override
  public boolean equals(Object obj) {
    return obj instanceof BranchOrCommitCell
        && element.equals(obj)
        && text.equals(((BranchOrCommitCell) obj).text)
        && printElements.equals(((BranchOrCommitCell) obj).printElements);
  }

  @Override
  public int hashCode() {
    int result = element.hashCode();
    result = 31 * result + text.hashCode();
    result = 37 * result + printElements.hashCode();
    return result;
  }
}
