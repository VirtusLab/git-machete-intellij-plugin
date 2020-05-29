package com.virtuslab.gitmachete.frontend.graph.impl.items;

import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;

@Getter(onMethod_ = {@Override})
@ToString
public abstract class BaseGraphItem implements IGraphItem {

  private final GraphItemColor color;

  private final @GTENegativeOne int prevSiblingItemIndex;

  private @MonotonicNonNull @Positive Integer nextSiblingItemIndex = null;

  private final @NonNegative int indentLevel;

  protected BaseGraphItem(
      GraphItemColor color,
      @GTENegativeOne int prevSiblingItemIndex,
      @Positive int nextSiblingItemIndex,
      @NonNegative int indentLevel) {
    this.color = color;
    this.prevSiblingItemIndex = prevSiblingItemIndex;
    this.nextSiblingItemIndex = nextSiblingItemIndex;
    this.indentLevel = indentLevel;
  }

  protected BaseGraphItem(
      GraphItemColor color,
      @GTENegativeOne int prevSiblingItemIndex,
      @NonNegative int indentLevel) {
    this.color = color;
    this.prevSiblingItemIndex = prevSiblingItemIndex;
    this.indentLevel = indentLevel;
  }

  @Override
  public @Nullable @Positive Integer getNextSiblingItemIndex() {
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
