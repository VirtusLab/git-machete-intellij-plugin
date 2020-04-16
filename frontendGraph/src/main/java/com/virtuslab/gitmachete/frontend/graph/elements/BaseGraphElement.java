package com.virtuslab.gitmachete.frontend.graph.elements;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
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

  @GTENegativeOne
  private final int upElementIndex;

  /** The index of the first node in the next sibling branch or its commit. */
  @Positive
  @MonotonicNonNull
  private Integer downElementIndex = null;

  @NonNegative
  private final int indentLevel;

  protected BaseGraphElement(@Nullable GraphEdgeColor graphEdgeColor,
      @GTENegativeOne int upElementIndex,
      @Positive int downElementIndex,
      @NonNegative int indentLevel) {
    this.graphEdgeColor = graphEdgeColor;
    this.upElementIndex = upElementIndex;
    this.downElementIndex = downElementIndex;
    this.indentLevel = indentLevel;
  }

  protected BaseGraphElement(@Nullable GraphEdgeColor graphEdgeColor,
      @GTENegativeOne int upElementIndex,
      @NonNegative int indentLevel) {
    this.graphEdgeColor = graphEdgeColor;
    this.upElementIndex = upElementIndex;
    this.indentLevel = indentLevel;
  }

  @Override
  @NonNegative
  public int getIndentLevel() {
    return indentLevel;
  }

  @Override
  public void setDownElementIndex(@Positive int i) {
    assert downElementIndex == null : "downElementIndex has already been set";
    downElementIndex = i;
  }
}
