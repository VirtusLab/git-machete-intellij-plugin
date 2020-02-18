package com.virtuslab.gitmachete.graph;

import static com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus.InSync;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus.InSyncButForkPointOff;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus.Merged;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus.OutOfSync;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.GRAY;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.GREEN;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.RED;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.YELLOW;

import com.intellij.ui.JBColor;
import com.intellij.vcs.log.paint.ColorGenerator;
import java.awt.Color;
import java.util.Map;

public class SyncToParentStatusEdgeColorGenerator implements ColorGenerator {

  private static final Map<Integer, JBColor> colors =
      Map.of(
          Merged.getId(), GRAY,
          InSyncButForkPointOff.getId(), YELLOW,
          OutOfSync.getId(), RED,
          InSync.getId(), GREEN);

  @Override
  public Color getColor(int statusId) {
    return colors.getOrDefault(statusId, JBColor.GRAY);
  }
}
