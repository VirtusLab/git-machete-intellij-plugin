package com.virtuslab.gitmachete.frontend.graph.nodes;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;

@EqualsAndHashCode(callSuper = true)
@Getter
public final class CommitNode extends BaseGraphNode {
  private final IGitMacheteCommit commit;
  @Positive
  private final int branchNodeIndex;

  public CommitNode(
      IGitMacheteCommit commit,
      GraphEdgeColor containingBranchGraphEdgeColor,
      @NonNegative int prevSiblingNodeIndex,
      @Positive int nextSiblingNodeIndex,
      @Positive int branchNodeIndex,
      @NonNegative int indentLevel) {
    super(containingBranchGraphEdgeColor, prevSiblingNodeIndex, nextSiblingNodeIndex, indentLevel);
    this.commit = commit;
    this.branchNodeIndex = branchNodeIndex;
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
  public boolean hasChildNode() {
    return false;
  }

  @Override
  public boolean isBranch() {
    return false;
  }
}
