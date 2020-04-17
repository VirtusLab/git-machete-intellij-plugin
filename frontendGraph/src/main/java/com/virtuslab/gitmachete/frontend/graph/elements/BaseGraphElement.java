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
  private final int prevSiblingElementIndex;

  @Positive
  @MonotonicNonNull
  private Integer nextSiblingElementIndex = null;

  @NonNegative
  private final int indentLevel;

  protected BaseGraphElement(@Nullable GraphEdgeColor graphEdgeColor,
      @GTENegativeOne int prevSiblingElementIndex,
      @Positive int nextSiblingElementIndex,
      @NonNegative int indentLevel) {
    this.graphEdgeColor = graphEdgeColor;
    this.prevSiblingElementIndex = prevSiblingElementIndex;
    this.nextSiblingElementIndex = nextSiblingElementIndex;
    this.indentLevel = indentLevel;
  }

  protected BaseGraphElement(@Nullable GraphEdgeColor graphEdgeColor,
      @GTENegativeOne int prevSiblingElementIndex,
      @NonNegative int indentLevel) {
    this.graphEdgeColor = graphEdgeColor;
    this.prevSiblingElementIndex = prevSiblingElementIndex;
    this.indentLevel = indentLevel;
  }

  @Override
  @NonNegative
  public int getIndentLevel() {
    return indentLevel;
  }

  @Override
  public void setNextSiblingElementIndex(@Positive int i) {
    assert nextSiblingElementIndex == null : "nextSiblingElementIndex has already been set";
    nextSiblingElementIndex = i;
  }
}
