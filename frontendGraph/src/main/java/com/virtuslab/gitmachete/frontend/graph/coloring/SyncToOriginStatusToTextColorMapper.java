package com.virtuslab.gitmachete.frontend.graph.coloring;

import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Ahead;
import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Behind;
import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Diverged;
import static com.virtuslab.gitmachete.backend.api.SyncToOriginStatus.Untracked;
import static com.virtuslab.gitmachete.frontend.graph.coloring.ColorDefinitions.ORANGE;
import static com.virtuslab.gitmachete.frontend.graph.coloring.ColorDefinitions.RED;

import java.util.Map;

import com.intellij.ui.JBColor;

public final class SyncToOriginStatusToTextColorMapper {
  private SyncToOriginStatusToTextColorMapper() {}

  private static final Map<Integer, JBColor> COLORS = Map.of(
      Untracked.getId(), ORANGE,
      Ahead.getId(), RED,
      Behind.getId(), RED,
      Diverged.getId(), RED);

  public static JBColor getColor(int statusId) {
    return COLORS.getOrDefault(statusId, JBColor.GRAY);
  }
}
