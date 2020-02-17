package com.virtuslab.gitmachete.graph.model;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.SmartList;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@RequiredArgsConstructor
@Getter
public abstract class BaseGraphElement implements IGraphElement {
  private static final String EMPTY_VALUE = "";

  private final int upElementIndex;

  /** For {@link CommitElement} this is status of the branch that contains the commit. */
  private final SyncToParentStatus syncToParentStatus;

  /*
   * Final (reference initialized once),
   * but in some cases downElementIndexes are not known while instance construction
   * and they have to be added later.
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
}
