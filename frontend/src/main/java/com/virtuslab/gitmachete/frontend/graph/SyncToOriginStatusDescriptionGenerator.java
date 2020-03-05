package com.virtuslab.gitmachete.frontend.graph;

import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Ahead;
import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Behind;
import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Diverged;
import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Untracked;

import java.util.Map;

public final class SyncToOriginStatusDescriptionGenerator {
  private SyncToOriginStatusDescriptionGenerator() {}

  private static final Map<Integer, String> descriptions = Map.of(
      Untracked.getId(), "untracked",
      Ahead.getId(), "ahead of origin",
      Behind.getId(), "behind origin",
      Diverged.getId(), "diverged from origin");

  public static String getDescription(int statusId) {
    return descriptions.getOrDefault(statusId, "sync to origin unknown");
  }
}
