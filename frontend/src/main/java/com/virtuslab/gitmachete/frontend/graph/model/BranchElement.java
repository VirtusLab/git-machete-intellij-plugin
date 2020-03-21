package com.virtuslab.gitmachete.frontend.graph.model;

import java.awt.Color;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;
import com.virtuslab.gitmachete.frontend.graph.GraphEdgeColor;

@EqualsAndHashCode(callSuper = true)
@Getter
public final class BranchElement extends BaseGraphElement {
  private final IGitMacheteBranch branch;
  private final SyncToOriginStatus syncToOriginStatus;

  public BranchElement(
      IGitMacheteBranch branch,
      int upElementIndex,
      GraphEdgeColor graphEdgeColor,
      SyncToOriginStatus syncToOriginStatus,
      boolean isCurrentBranch) {
    super(upElementIndex, graphEdgeColor);
    this.branch = branch;
    this.syncToOriginStatus = syncToOriginStatus;
    this.attributes = isCurrentBranch ? UNDERLINE_BOLD_ATTRIBUTES : NORMAL_ATTRIBUTES;
  }

  private static final JBColor branchTextColor = new JBColor(Color.BLACK, Color.WHITE);

  private static final SimpleTextAttributes UNDERLINE_BOLD_ATTRIBUTES = new SimpleTextAttributes(
      SimpleTextAttributes.STYLE_UNDERLINE | SimpleTextAttributes.STYLE_BOLD, branchTextColor);

  private static final SimpleTextAttributes NORMAL_ATTRIBUTES = new SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN, branchTextColor);

  @Getter
  private final SimpleTextAttributes attributes;

  @Override
  public String getValue() {
    return branch.getName();
  }

  @Override
  public boolean hasBulletPoint() {
    return true;
  }

  @Override
  public boolean isBranch() {
    return true;
  }
}
