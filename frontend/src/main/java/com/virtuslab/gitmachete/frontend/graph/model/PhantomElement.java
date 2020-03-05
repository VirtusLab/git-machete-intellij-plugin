package com.virtuslab.gitmachete.frontend.graph.model;

// TODO (#90): find and apply a proper approach for indents handling

import com.virtuslab.gitmachete.frontend.graph.GraphEdgeColor;

/**
 * An element whose purpose is to keep the child branch ({@link CommitElement}s and theirs {@link BranchElement})
 * horizontally aligned (shifts right the last one). This element is not intended to be shown. This is a hackish
 * solution to solve the problem. It should be treated as temporary and an appropriate one shall be implemented (own
 * {@link com.intellij.vcs.log.graph.api.printer.PrintElementGenerator} might be needed, #90).
 *
 * <p>
 * Note that only the last child branch of any given parent branch needs a phantom element (for the rest the indent is
 * gained with {@link SplittingElement}). This is because the edge connecting all splitting elements in branch is the
 * actual indent cause. The phantom element is an extension of this edge.
 */
public class PhantomElement extends BaseGraphElement {
  public PhantomElement(int upElementIndex) {
    super(upElementIndex, /* syncToParentStatus */ null);
  }

  @Override
  public GraphEdgeColor getGraphEdgeColor() {
    return GraphEdgeColor.TRANSPARENT;
  }
}
