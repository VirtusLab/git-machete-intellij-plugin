package com.virtuslab.gitmachete.frontend.graph.elements;

import com.intellij.ui.SimpleTextAttributes;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;

@Getter
@EqualsAndHashCode
@ToString
public abstract class BaseGraphElement implements IGraphElement {
  private static final String EMPTY_VALUE = "";

  @Nullable
  private final GraphEdgeColor graphEdgeColor;

  private final int upElementIndex;

  /** The index of the first node in the next sibling branch or its commit. */
  @MonotonicNonNull
  private Integer downElementIndex = null;

  private final int indentLevel;

  protected BaseGraphElement(@Nullable GraphEdgeColor graphEdgeColor, int upElementIndex, int downElementIndex,
      int indentLevel) {
    this.graphEdgeColor = graphEdgeColor;
    this.upElementIndex = upElementIndex;
    this.downElementIndex = downElementIndex;
    this.indentLevel = indentLevel;
  }

  protected BaseGraphElement(@Nullable GraphEdgeColor graphEdgeColor, int upElementIndex, int indentLevel) {
    this.graphEdgeColor = graphEdgeColor;
    this.upElementIndex = upElementIndex;
    this.indentLevel = indentLevel;
  }

  @Override
  public String getValue() {
    return EMPTY_VALUE;
  }

  @Override
  public SimpleTextAttributes getAttributes() {
    return SimpleTextAttributes.REGULAR_ATTRIBUTES;
  }

  @Override
  public int getIndentLevel() {
    return indentLevel;
  }

  @Override
  public boolean hasBulletPoint() {
    return false;
  }

  @Override
  public boolean hasSubelement() {
    return false;
  }

  public void setDownElementIndex(int i) {
    assert downElementIndex == null : "downElementIndex has already been set";
    downElementIndex = i;
  }

  @Override
  public boolean isBranch() {
    return false;
  }
}
