package com.virtuslab.gitmachete.frontend.ui.cell;

import io.vavr.collection.List;
import lombok.Data;

import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;

@Data
public final class BranchOrCommitCell {
  private final IGraphItem graphItem;
  private final String text;
  private final List<? extends IRenderPart> renderParts;

  public BranchOrCommitCell(
      IGraphItem graphItem,
      List<? extends IRenderPart> renderParts) {
    this.text = graphItem.getValue();
    this.renderParts = renderParts;
    this.graphItem = graphItem;
  }
}
