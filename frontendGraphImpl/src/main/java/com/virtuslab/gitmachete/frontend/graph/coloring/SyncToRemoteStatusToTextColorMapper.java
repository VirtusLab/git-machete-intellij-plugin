package com.virtuslab.gitmachete.frontend.graph.coloring;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Ahead;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Behind;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Diverged;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;
import static com.virtuslab.gitmachete.frontend.graph.coloring.ColorDefinitions.ORANGE;
import static com.virtuslab.gitmachete.frontend.graph.coloring.ColorDefinitions.RED;

import com.intellij.ui.JBColor;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public final class SyncToRemoteStatusToTextColorMapper {
  private SyncToRemoteStatusToTextColorMapper() {}

  private static final Map<SyncToRemoteStatus.Relation, JBColor> COLORS = HashMap.of(
      Untracked, ORANGE,
      Ahead, RED,
      Behind, RED,
      Diverged, RED);

  public static JBColor getColor(SyncToRemoteStatus.Relation relation) {
    return COLORS.getOrElse(relation, JBColor.GRAY);
  }
}
