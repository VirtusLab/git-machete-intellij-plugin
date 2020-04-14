package com.virtuslab.gitmachete.frontend.graph.elements;

import com.intellij.ui.SimpleTextAttributes;

import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;

public interface IGraphElement {

  /**
   * @return The index of element above (in table) that is connected directly in graph to this one.
   */
  int getUpElementIndex();

  /**
   * @return Index of element below (in table) that is connected directly in graph to this one and that is not its subbranch.
   */
  Integer getDownElementIndex();

  void setDownElementIndex(int i);

  /** @return The text (commit message/branch name) to be displayed in the table. */
  String getValue();

  /** @return Attributes (eg. boldness) to be used by the displayed text. */
  SimpleTextAttributes getAttributes();

  GraphEdgeColor getGraphEdgeColor();

  int getIndentLevel();

  boolean hasBulletPoint();

  /** The subelement is an indented element (branch or commit). */
  boolean hasSubelement();

  boolean isBranch();
}
