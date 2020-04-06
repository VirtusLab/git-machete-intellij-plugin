package com.virtuslab.gitmachete.frontend.graph.elements;

import java.util.List;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.SmartList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
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

  /**
   * Final (reference initialized once), but in some cases {@code downElementIndexes} are not known during instance
   * construction and they have to be added later.
   */
  private final List<Integer> downElementIndexes;

  protected BaseGraphElement(@Nullable GraphEdgeColor graphEdgeColor, int upElementIndex) {
    this.graphEdgeColor = graphEdgeColor;
    this.upElementIndex = upElementIndex;
    this.downElementIndexes = new SmartList<Integer>();
  }

  protected BaseGraphElement(@Nullable GraphEdgeColor graphEdgeColor, int upElementIndex, int downElementIndex) {
    this.graphEdgeColor = graphEdgeColor;
    this.upElementIndex = upElementIndex;
    this.downElementIndexes = new SmartList<Integer>(downElementIndex);
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
  public boolean hasBulletPoint() {
    return false;
  }

  @Override
  public boolean isBranch() {
    return false;
  }
}
