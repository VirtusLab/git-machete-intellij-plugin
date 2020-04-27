package com.virtuslab.gitmachete.frontend.graph.impl.items;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import io.vavr.NotImplementedError;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.frontend.graph.api.coloring.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.items.IBranchItem;
import com.virtuslab.gitmachete.frontend.graph.api.items.ICommitItem;

@Getter
public final class CommitItem extends BaseGraphItem implements ICommitItem {
  private final IGitMacheteCommit commit;
  @Positive
  private final int branchItemIndex;

  public CommitItem(
      IGitMacheteCommit commit,
      GraphItemColor containingBranchGraphItemColor,
      @NonNegative int prevSiblingItemIndex,
      @Positive int nextSiblingItemIndex,
      @Positive int branchItemIndex,
      @NonNegative int indentLevel) {
    super(containingBranchGraphItemColor, prevSiblingItemIndex, nextSiblingItemIndex, indentLevel);
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
  public IBranchItem asBranchItem() {
    throw new NotImplementedError();
  }

  @Override
  public ICommitItem asCommitItem() {
    return this;
  }
}
