package com.virtuslab.gitmachete.frontend.graph.items;

import java.awt.Color;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import io.vavr.NotImplementedError;
import lombok.Getter;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.graph.coloring.GraphItemColor;

@Getter
public final class BranchItem extends BaseGraphItem {
  private final BaseGitMacheteBranch branch;
  private final SyncToRemoteStatus syncToRemoteStatus;
  private final SimpleTextAttributes attributes;
  private final boolean hasChildItem;

  public BranchItem(
      BaseGitMacheteBranch branch,
      GraphItemColor graphItemColor,
      SyncToRemoteStatus syncToRemoteStatus,
      @GTENegativeOne int prevSiblingItemIndex,
      @NonNegative int indentLevel,
      boolean isCurrentBranch,
      boolean hasChildItem) {
    super(graphItemColor, prevSiblingItemIndex, indentLevel);
    this.branch = branch;
    this.syncToRemoteStatus = syncToRemoteStatus;
    this.attributes = isCurrentBranch ? UNDERLINE_BOLD_ATTRIBUTES : NORMAL_ATTRIBUTES;
    this.hasChildItem = hasChildItem;
  }

  private static final JBColor BRANCH_TEXT_COLOR = new JBColor(Color.BLACK, Color.WHITE);

  private static final SimpleTextAttributes UNDERLINE_BOLD_ATTRIBUTES = new SimpleTextAttributes(
      SimpleTextAttributes.STYLE_UNDERLINE | SimpleTextAttributes.STYLE_BOLD, BRANCH_TEXT_COLOR);

  private static final SimpleTextAttributes NORMAL_ATTRIBUTES = new SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN, BRANCH_TEXT_COLOR);

  @Override
  public String getValue() {
    return branch.getName();
  }

  @Override
  public boolean hasBulletPoint() {
    return true;
  }

  @Override
  public boolean hasChildItem() {
    return hasChildItem;
  }

  @Override
  public boolean isBranchItem() {
    return true;
  }

  @Override
  public BranchItem asBranchItem() {
    return this;
  }

  @Override
  public CommitItem asCommitItem() {
    throw new NotImplementedError();
  }
}
