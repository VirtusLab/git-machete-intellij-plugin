package com.virtuslab.gitmachete.frontend.graph.elements;

import com.intellij.ui.SimpleTextAttributes;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;

public interface IGraphElement {

  /**
   * @return The index of element above (in table) that is connected directly
   * and at the same indent level in graph to this one.
   */
  @GTENegativeOne
  int getUpElementIndex();

  /**
   * @return Index of element below (in table) that is connected directly
   * and at the same indent level in graph to this one (hence it is not its subbranch).
   * <ul>
   *     <li>for a commit element, it's either another commit element or the containing branch (never null),</li>
   *     <li>for a branch element, it's either next sibling branch element or null (if none left)</li>
   * </ul>
   */
  @Positive
  Integer getDownElementIndex();

  void setDownElementIndex(@Positive int i);

  /** @return The text (commit message/branch name) to be displayed in the table. */
  String getValue();

  /** @return Attributes (eg. boldness) to be used by the displayed text. */
  SimpleTextAttributes getAttributes();

  GraphEdgeColor getGraphEdgeColor();

  @NonNegative
  int getIndentLevel();

  boolean hasBulletPoint();

  /** The subelement is an indented element (branch or commit). */
  boolean hasSubelement();

  boolean isBranch();
}
