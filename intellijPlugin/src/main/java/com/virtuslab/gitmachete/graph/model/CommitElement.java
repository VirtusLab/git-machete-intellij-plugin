package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
public final class CommitElement extends BaseGraphElement {
  @Getter private final IGitMacheteCommit commit;
  @Getter private final int branchElementIndex;

  public CommitElement(
      IGitMacheteCommit commit,
      int upElementIndex,
      int downElementIndex,
      int branchElementIndex,
      SyncToParentStatus containingBranchSyncToParentStatus) {
    super(upElementIndex, containingBranchSyncToParentStatus);
    this.commit = commit;
    this.branchElementIndex = branchElementIndex;
    getDownElementIndexes().add(downElementIndex);
  }

  @Override
  public String getValue() {
    return commit.getMessage();
  }

  @Override
  public SimpleTextAttributes getAttributes() {
    return new SimpleTextAttributes(
        SimpleTextAttributes.STYLE_ITALIC | SimpleTextAttributes.STYLE_SMALLER,
        UIUtil.getInactiveTextColor());
  }
}
