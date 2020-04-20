package com.virtuslab.gitmachete.frontend.graph.nodes;

import java.awt.Color;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Getter;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;
@Getter
public final class BranchNode extends BaseGraphNode {
  private final BaseGitMacheteBranch branch;
  private final ISyncToRemoteStatus syncToRemoteStatus;
  private final SimpleTextAttributes attributes;
  private final boolean hasChildNode;

  public BranchNode(
          BaseGitMacheteBranch branch,
          GraphEdgeColor graphEdgeColor,
          ISyncToRemoteStatus syncToRemoteStatus,
          int prevSiblingNodeIndex,
          int indentLevel,
          boolean isCurrentBranch,
          boolean hasChildNode) {
    super(graphEdgeColor, prevSiblingNodeIndex, indentLevel);
    this.branch = branch;
    this.syncToRemoteStatus = syncToRemoteStatus;
    this.attributes = isCurrentBranch ? UNDERLINE_BOLD_ATTRIBUTES : NORMAL_ATTRIBUTES;
    this.hasChildNode = hasChildNode;
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
  public boolean hasChildNode() {
    return hasChildNode;
  }

  @Override
  public boolean isBranch() {
    return true;
  }
}
