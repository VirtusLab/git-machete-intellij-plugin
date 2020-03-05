package com.virtuslab.gitmachete.frontend.graph;

import java.util.Map;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

public final class SyncToParentStatusToGraphEdgeColorMapper {
  private SyncToParentStatusToGraphEdgeColorMapper() {}

  private static final Map<SyncToParentStatus, GraphEdgeColor> edges = Map.of(
      SyncToParentStatus.Merged, GraphEdgeColor.GRAY,
      SyncToParentStatus.InSyncButForkPointOff, GraphEdgeColor.YELLOW,
      SyncToParentStatus.OutOfSync, GraphEdgeColor.RED,
      SyncToParentStatus.InSync, GraphEdgeColor.GREEN);

  public static GraphEdgeColor getGraphEdgeColor(SyncToParentStatus syncToParentStatus) {
    return edges.getOrDefault(syncToParentStatus, GraphEdgeColor.TRANSPARENT);
  }
}
