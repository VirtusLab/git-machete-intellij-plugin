package com.virtuslab.gitmachete.frontend.graph.nodes;

import com.intellij.ui.SimpleTextAttributes;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public interface IGraphNode {

  /**
   * @return the index of previous sibling node (above in table) that is connected directly
   * and thus at the same indent level in graph to this one.
   */
  @GTENegativeOne
  int getPrevSiblingNodeIndex();

  /**
   * @return the index of next sibling node (below in table) that is connected directly
   * and thus at the same indent level in graph to this one (hence it is not its child node).
   * <ul>
   *     <li>for a commit node, it's either another commit node or the containing branch (never null),</li>
   *     <li>for a branch node, it's either next sibling branch node or null (if none left)</li>
   * </ul>
   */
  @Positive
  @MonotonicNonNull
  Integer getNextSiblingNodeIndex();

  void setNextSiblingNodeIndex(@Positive int i);

  /** @return the text (commit message/branch name) to be displayed in the table */
  String getValue();

  /** @return the attributes (eg. boldness) to be used by the displayed text */
  SimpleTextAttributes getAttributes();

  GraphEdgeColor getGraphEdgeColor();

  @NonNegative
  int getIndentLevel();

  boolean hasBulletPoint();

  /** @return the childNode which is an indented node (branch or commit) */
  boolean hasChildNode();

  boolean isBranch();
}
