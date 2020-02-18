package com.virtuslab.gitmachete.graph;

import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Ahead;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Behind;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Diverged;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Untracked;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.ORANGE;
import static com.virtuslab.gitmachete.graph.ColorDefinitions.RED;

import com.intellij.ui.JBColor;
import com.intellij.vcs.log.paint.ColorGenerator;
import java.awt.Color;
import java.util.Map;

public class SyncToOriginStatusTextColorGenerator implements ColorGenerator {

  private static final Map<Integer, JBColor> colors =
      Map.of(
          Untracked.getId(), ORANGE,
          Ahead.getId(), RED,
          Behind.getId(), RED,
          Diverged.getId(), RED);

  @Override
  public Color getColor(int statusId) {
    return colors.getOrDefault(statusId, JBColor.GRAY);
  }
}
