package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
public final class CommitElement extends BaseGraphElement {
  @Getter private final IGitMacheteCommit commit;
  /* index of this commit containing graph */
  @Getter private final int branchIndex;

  public CommitElement(
      IGitMacheteCommit commit,
      IGitMacheteBranch branch,
      int upElementIndex,
      int branchIndex,
      int downElementIndex) {
    super(branch, upElementIndex);
    this.commit = commit;
    this.branchIndex = branchIndex;
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
