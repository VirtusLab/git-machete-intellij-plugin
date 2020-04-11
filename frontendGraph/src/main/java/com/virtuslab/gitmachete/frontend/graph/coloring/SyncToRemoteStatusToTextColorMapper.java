package com.virtuslab.gitmachete.frontend.graph.coloring;

import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Ahead;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Behind;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Diverged;
import static com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus.Relation.Untracked;
import static com.virtuslab.gitmachete.frontend.graph.coloring.ColorDefinitions.ORANGE;
import static com.virtuslab.gitmachete.frontend.graph.coloring.ColorDefinitions.RED;

import com.intellij.ui.JBColor;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;

public final class SyncToRemoteStatusToTextColorMapper {
  private SyncToRemoteStatusToTextColorMapper() {}

  private static final Map<ISyncToRemoteStatus.Relation, JBColor> COLORS = HashMap.of(
      Untracked, ORANGE,
      Ahead, RED,
      Behind, RED,
      Diverged, RED);

  public static JBColor getColor(ISyncToRemoteStatus.Relation relation) {
    return COLORS.getOrElse(relation, JBColor.GRAY);
  }
}
