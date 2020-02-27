package com.virtuslab.gitmachete.graph;

import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.util.Map;

public class SyncToParentStatusGraphEdgeColorGenerator {
  private static final Map<SyncToParentStatus, GraphEdgeColor> edges =
      Map.of(
          SyncToParentStatus.Merged, GraphEdgeColor.GRAY,
          SyncToParentStatus.InSyncButForkPointOff, GraphEdgeColor.YELLOW,
          SyncToParentStatus.OutOfSync, GraphEdgeColor.RED,
          SyncToParentStatus.InSync, GraphEdgeColor.GREEN);

  public static GraphEdgeColor getGraphEdgeColor(SyncToParentStatus syncToParentStatus) {
    return edges.getOrDefault(syncToParentStatus, GraphEdgeColor.TRANSPARENT);
  }
}
