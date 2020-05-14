package com.virtuslab.gitmachete.frontend.graph.api.coloring;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;
import static com.virtuslab.gitmachete.frontend.graph.api.coloring.ColorDefinitions.ORANGE;
import static com.virtuslab.gitmachete.frontend.graph.api.coloring.ColorDefinitions.RED;

import com.intellij.ui.JBColor;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;

public final class SyncToRemoteStatusToTextColorMapper {
  private SyncToRemoteStatusToTextColorMapper() {}

  private static final Map<SyncToRemoteStatus.Relation, JBColor> COLORS = HashMap.of(
      Untracked, ORANGE,
      AheadOfRemote, RED,
      BehindRemote, RED,
      DivergedFromAndNewerThanRemote, RED,
      DivergedFromAndOlderThanRemote, RED);

  public static JBColor getColor(SyncToRemoteStatus.Relation relation) {
    return COLORS.getOrElse(relation, JBColor.GRAY);
  }
}
