package com.virtuslab.gitmachete.frontend.graph.model;

import java.util.List;

import lombok.Data;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.SmartList;

import com.virtuslab.gitmachete.frontend.graph.GraphEdgeColor;

@Data
public abstract class BaseGraphElement implements IGraphElement {
  private static final String EMPTY_VALUE = "";

  private final int upElementIndex;

  private final GraphEdgeColor graphEdgeColor;

  /**
   * Final (reference initialized once), but in some cases {@code downElementIndexes} are not known while instance
   * construction and they have to be added later.
   */
  private final List<Integer> downElementIndexes = new SmartList<>();

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
