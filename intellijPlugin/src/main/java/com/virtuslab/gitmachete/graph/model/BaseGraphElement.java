package com.virtuslab.gitmachete.graph.model;

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
  private final int upElementIndex;
  private final SyncToParentStatus computedSyncToParentStatus;

  /*
   * Final (reference initialized once),
   * but in some cases downElementIndexes are not known while instance construction
   * and they have to be added later.
   */
  private final List<Integer> downElementIndexes = new SmartList<>();
}
