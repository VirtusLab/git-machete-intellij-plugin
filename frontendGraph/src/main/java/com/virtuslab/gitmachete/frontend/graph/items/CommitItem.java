package com.virtuslab.gitmachete.frontend.graph.items;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import io.vavr.NotImplementedError;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;

@Getter
public final class CommitItem extends BaseGraphItem {
  private final IGitMacheteCommit commit;
  @Positive
  private final int branchItemIndex;

  public CommitItem(
      IGitMacheteCommit commit,
      GraphEdgeColor containingBranchGraphEdgeColor,
      @NonNegative int prevSiblingItemIndex,
      @Positive int nextSiblingItemIndex,
      @Positive int branchItemIndex,
      @NonNegative int indentLevel) {
    super(containingBranchGraphEdgeColor, prevSiblingItemIndex, nextSiblingItemIndex, indentLevel);
    this.commit = commit;
    this.branchItemIndex = branchItemIndex;
  }

  @Override
  public SimpleTextAttributes getAttributes() {
    return new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC | SimpleTextAttributes.STYLE_SMALLER,
        UIUtil.getInactiveTextColor());
  }

  @Override
  public String getValue() {
    return commit.getMessage();
  }

  @Override
  public boolean hasBulletPoint() {
    return false;
  }

  @Override
  public boolean hasChildItem() {
    return false;
  }

  @Override
  public boolean isBranchItem() {
    return false;
  }

  @Override
  public BranchItem asBranchItem() {
    throw new NotImplementedError();
  }

  @Override
  public CommitItem asCommitItem() {
    return this;
  }
}
