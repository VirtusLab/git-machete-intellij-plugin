package com.virtuslab.gitmachete.frontend.graph.coloring;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Status.Ahead;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Status.Behind;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Status.Diverged;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Status.Untracked;
import static com.virtuslab.gitmachete.frontend.graph.coloring.ColorDefinitions.ORANGE;
import static com.virtuslab.gitmachete.frontend.graph.coloring.ColorDefinitions.RED;

import java.util.Map;

import com.intellij.ui.JBColor;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public final class SyncToRemoteStatusToTextColorMapper {
  private SyncToRemoteStatusToTextColorMapper() {}

  private static final Map<SyncToRemoteStatus.Status, JBColor> COLORS = Map.of(
      Untracked, ORANGE,
      Ahead, RED,
      Behind, RED,
      Diverged, RED);

  public static JBColor getColor(SyncToRemoteStatus.Status status) {
    return COLORS.getOrDefault(status, JBColor.GRAY);
  }
}
