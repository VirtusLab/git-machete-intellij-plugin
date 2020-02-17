package com.virtuslab.gitmachete.graph;

import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Ahead;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Behind;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Diverged;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Untracked;

import java.util.Map;

public class SyncToOriginStatusDescriptionGenerator {

  private static final Map<Integer, String> gitMacheteDescriptions =
      Map.of(
          Untracked.getId(), "untracked",
          Ahead.getId(), "ahead of origin",
          Behind.getId(), "behind of origin",
          Diverged.getId(), "diverged from origin");

  public static String getDescription(int statusId) {
    return gitMacheteDescriptions.getOrDefault(statusId, "sync to origin unknown");
  }
}
