package com.virtuslab.gitmachete.frontend.graph.elements;

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
public final class CommitElement extends BaseGraphElement {
  private final IGitMacheteCommit commit;
  @Positive
  private final int branchElementIndex;

  public CommitElement(
      IGitMacheteCommit commit,
      GraphEdgeColor containingBranchGraphEdgeColor,
      @NonNegative int upElementIndex,
      @Positive int downElementIndex,
      @Positive int branchElementIndex,
      @NonNegative int indentLevel) {
    super(containingBranchGraphEdgeColor, upElementIndex, downElementIndex, indentLevel);
    this.commit = commit;
    this.branchElementIndex = branchElementIndex;
  }

  @Override
  public String getValue() {
    return commit.getMessage();
  }

  @Override
  public SimpleTextAttributes getAttributes() {
    return new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC | SimpleTextAttributes.STYLE_SMALLER,
        UIUtil.getInactiveTextColor());
  }
}
