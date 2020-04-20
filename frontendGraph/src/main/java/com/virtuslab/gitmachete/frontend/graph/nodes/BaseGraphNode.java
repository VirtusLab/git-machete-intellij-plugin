package com.virtuslab.gitmachete.frontend.graph.nodes;

import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColor;

@Getter
@ToString
public abstract class BaseGraphNode implements IGraphNode {
  private static final String EMPTY_VALUE = "";

  @Nullable
  private final GraphEdgeColor graphEdgeColor;

  @GTENegativeOne
  private final int prevSiblingNodeIndex;

  @Positive
  @MonotonicNonNull
  private Integer nextSiblingNodeIndex = null;

  @NonNegative
  private final int indentLevel;

  protected BaseGraphNode(@Nullable GraphEdgeColor graphEdgeColor,
      @GTENegativeOne int prevSiblingNodeIndex,
      @Positive int nextSiblingNodeIndex,
      @NonNegative int indentLevel) {
    this.graphEdgeColor = graphEdgeColor;
    this.prevSiblingNodeIndex = prevSiblingNodeIndex;
    this.nextSiblingNodeIndex = nextSiblingNodeIndex;
    this.indentLevel = indentLevel;
  }

  protected BaseGraphNode(@Nullable GraphEdgeColor graphEdgeColor,
      @GTENegativeOne int prevSiblingNodeIndex,
      @NonNegative int indentLevel) {
    this.graphEdgeColor = graphEdgeColor;
    this.prevSiblingNodeIndex = prevSiblingNodeIndex;
    this.indentLevel = indentLevel;
  }

  @Override
  @NonNegative
  public int getIndentLevel() {
    return indentLevel;
  }

  @Override
  public void setNextSiblingNodeIndex(@Positive int i) {
    assert nextSiblingNodeIndex == null : "nextSiblingNodeIndex has already been set";
    nextSiblingNodeIndex = i;
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    return this == other;
  }

  @Override
  public final int hashCode() {
    return (((((graphEdgeColor != null ? graphEdgeColor.hashCode() : 0) * 23
        + (nextSiblingNodeIndex != null ? nextSiblingNodeIndex : 0)) * 23
        + prevSiblingNodeIndex) * 23)
        + indentLevel);
  }
}
