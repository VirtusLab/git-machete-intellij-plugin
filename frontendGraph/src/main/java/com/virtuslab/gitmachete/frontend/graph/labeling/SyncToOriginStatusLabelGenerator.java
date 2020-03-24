package com.virtuslab.gitmachete.frontend.graph.labeling;

import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Ahead;
import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Behind;
import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Diverged;
import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Untracked;

import java.util.Map;

public final class SyncToOriginStatusLabelGenerator {
  private SyncToOriginStatusLabelGenerator() {}

  private static final Map<Integer, String> labels = Map.of(
      Untracked.getId(), "untracked",
      Ahead.getId(), "ahead of origin",
      Behind.getId(), "behind origin",
      Diverged.getId(), "diverged from origin");

  public static String getLabel(int statusId) {
    return labels.getOrDefault(statusId, "sync to origin unknown");
  }
}
