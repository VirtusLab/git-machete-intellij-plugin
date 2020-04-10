package com.virtuslab.gitmachete.frontend.graph.labeling;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Status.Ahead;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Status.Behind;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Status.Diverged;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Status.Untracked;

import java.util.Map;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public final class SyncToRemoteStatusLabelGenerator {
  private SyncToRemoteStatusLabelGenerator() {}

  private static final Map<SyncToRemoteStatus.Status, String> LABELS = Map.of(
      Untracked, "untracked",
      Ahead, "ahead of %s",
      Behind, "behind %s",
      Diverged, "diverged from %s");

  @SuppressWarnings("format.string.invalid")
  public static String getLabel(SyncToRemoteStatus.Status status, String remoteName) {
    return String.format(LABELS.getOrDefault(status, "sync to %s unknown"), remoteName);
  }
}
