package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
public final class CommitElement extends BaseGraphElement {
  @Getter private final IGitMacheteCommit commit;

  public CommitElement(
      IGitMacheteCommit commit,
      int upElementIndex,
      int downElementIndex,
      SyncToParentStatus containingBranchSyncToParentStatus) {
    super(upElementIndex, containingBranchSyncToParentStatus);
    this.commit = commit;
    getDownElementIndexes().add(downElementIndex);
  }

  @Override
  public String getValue() {
    return commit.getMessage();
  }

  @Override
  public SimpleTextAttributes getAttributes() {
    return SimpleTextAttributes.GRAYED_ATTRIBUTES;
  }
}
