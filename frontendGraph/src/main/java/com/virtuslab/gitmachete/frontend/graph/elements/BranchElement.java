package com.virtuslab.gitmachete.frontend.graph.elements;

import java.awt.Color;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;

@EqualsAndHashCode(callSuper = true)
@Getter
public final class BranchElement extends BaseGraphElement {
  private final BaseGitMacheteBranch branch;
  private final ISyncToRemoteStatus syncToRemoteStatus;
  private final SimpleTextAttributes attributes;
  private final boolean hasSubelement;

  public BranchElement(
      BaseGitMacheteBranch branch,
      GraphEdgeColor graphEdgeColor,
      int upElementIndex,
      ISyncToRemoteStatus syncToRemoteStatus,
      boolean isCurrentBranch,
      int indentLevel,
      boolean hasSubelement) {
    super(graphEdgeColor, upElementIndex, indentLevel);
    this.branch = branch;
    this.syncToRemoteStatus = syncToRemoteStatus;
    this.attributes = isCurrentBranch ? UNDERLINE_BOLD_ATTRIBUTES : NORMAL_ATTRIBUTES;
    this.hasSubelement = hasSubelement;
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
  public boolean hasSubelement() {
    return hasSubelement;
  }

  @Override
  public boolean isBranch() {
    return true;
  }
}
