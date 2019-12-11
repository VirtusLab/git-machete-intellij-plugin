package com.virtuslab.gitmachete.ui.render;

import com.intellij.vcs.log.graph.PrintElement;
import java.util.Collection;
import javax.annotation.Nonnull;
import lombok.Getter;

public class GraphBranchCell {
  @Getter @Nonnull private final String text;

  @Getter @Nonnull private final Collection<? extends PrintElement> printElements;

  public GraphBranchCell(
      @Nonnull String text, @Nonnull Collection<? extends PrintElement> printElements) {
    this.text = text;
    this.printElements = printElements;
  }

  @Override
  public String toString() {
    return text;
  }
}
