package com.virtuslab.gitmachete.frontend.graph.api.coloring;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

public final class SyncToParentStatusToGraphItemColorMapper {
  private SyncToParentStatusToGraphItemColorMapper() {}

  private static final Map<SyncToParentStatus, GraphItemColor> ITEM_COLORS = HashMap.of(
      SyncToParentStatus.MergedToParent, GraphItemColor.GRAY,
      SyncToParentStatus.InSyncButForkPointOff, GraphItemColor.YELLOW,
      SyncToParentStatus.OutOfSync, GraphItemColor.RED,
      SyncToParentStatus.InSync, GraphItemColor.GREEN);

  public static GraphItemColor getGraphItemColor(SyncToParentStatus syncToParentStatus) {
    return ITEM_COLORS.getOrElse(syncToParentStatus, GraphItemColor.TRANSPARENT);
  }
}
