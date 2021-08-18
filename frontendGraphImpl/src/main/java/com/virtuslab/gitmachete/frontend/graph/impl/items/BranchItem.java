package com.virtuslab.gitmachete.frontend.graph.impl.items;

import java.awt.Color;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AccessLevel;
import lombok.Getter;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;
import com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.items.IBranchItem;

@Getter
public final class BranchItem extends BaseGraphItem implements IBranchItem {
  private final IManagedBranchSnapshot branch;
  private final RelationToRemote relationToRemote;
  private final SimpleTextAttributes attributes;
  private final boolean isCurrentBranch;

  @Getter(AccessLevel.NONE)
  private final boolean hasChildItem;

  public BranchItem(
      IManagedBranchSnapshot branch,
      GraphItemColor graphItemColor,
      RelationToRemote relationToRemote,
      @GTENegativeOne int prevSiblingItemIndex,
      @NonNegative int indentLevel,
      boolean isCurrentBranch,
      boolean hasChildItem) {
    super(graphItemColor, prevSiblingItemIndex, indentLevel);
    this.branch = branch;
    this.relationToRemote = relationToRemote;
    this.attributes = isCurrentBranch ? UNDERLINE_BOLD_ATTRIBUTES : NORMAL_ATTRIBUTES;
    this.isCurrentBranch = isCurrentBranch;
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
}
