package com.virtuslab.gitmachete.frontend.graph.impl.items;

import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.graph.api.coloring.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;

@Getter
@ToString
public abstract class BaseGraphItem implements IGraphItem {

  private final GraphItemColor graphItemColor;

  @GTENegativeOne
  private final int prevSiblingItemIndex;

  @Positive
  @MonotonicNonNull
  private Integer nextSiblingItemIndex = null;

  @NonNegative
  private final int indentLevel;

  protected BaseGraphItem(GraphItemColor graphItemColor,
      @GTENegativeOne int prevSiblingItemIndex,
      @Positive int nextSiblingItemIndex,
      @NonNegative int indentLevel) {
    this.graphItemColor = graphItemColor;
    this.prevSiblingItemIndex = prevSiblingItemIndex;
    this.nextSiblingItemIndex = nextSiblingItemIndex;
    this.indentLevel = indentLevel;
  }

  protected BaseGraphItem(GraphItemColor graphItemColor,
      @GTENegativeOne int prevSiblingItemIndex,
      @NonNegative int indentLevel) {
    this.graphItemColor = graphItemColor;
    this.prevSiblingItemIndex = prevSiblingItemIndex;
    this.indentLevel = indentLevel;
  }

  @Override
  @Nullable
  @Positive
  public Integer getNextSiblingItemIndex() {
    return this.nextSiblingItemIndex;
  }

  @Override
  public void setNextSiblingItemIndex(@Positive int i) {
    assert nextSiblingItemIndex == null : "nextSiblingItemIndex has already been set";
    nextSiblingItemIndex = i;
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    return this == other;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }
}
