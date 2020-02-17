package com.virtuslab.gitmachete.graph.model;

import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;

/** Graph element that is an intersection for two same level (indent) branches. */
public class SplittingElement extends BaseGraphElement {
  public SplittingElement(
      int upElementIndex, int downElementIndex, SyncToParentStatus syncToParentStatus) {
    super(upElementIndex, syncToParentStatus);
    getDownElementIndexes().add(downElementIndex);
  }
}
