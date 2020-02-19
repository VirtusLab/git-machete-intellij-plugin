package com.virtuslab.gitmachete.graph.model;

import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;

/**
 * Graph element that precedes every non-root branch (or {@link CommitElement}s and their {@link
 * BranchElement}). It is a separator (increases spacing). In case of graph without commits, it is
 * an attach node for next (sibling) branch (for a graph with commits an attach node would be that
 * last {@link CommitElement}). In particular, it is an intersection for two direct children of the
 * same parent branch (siblings). For a given parent branch all its splitting elements are in the
 * same line horizontally (with the parent, and each other). Thus all its child branches will be in
 * the same line too, but one unit right from their splitting elements (indented). The only
 * exception from it is the last branch which is not indented because of lack the of next (sibling)
 * branch. The indent for it is handled with {@link PhantomElement}.
 */
public class SplittingElement extends BaseGraphElement {
  public SplittingElement(
      int upElementIndex, int downElementIndex, SyncToParentStatus syncToParentStatus) {
    super(upElementIndex, syncToParentStatus);
    getDownElementIndexes().add(downElementIndex);
  }
}
