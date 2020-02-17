package com.virtuslab.gitmachete.graph;

import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Ahead;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Behind;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Diverged;
import static com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus.Untracked;

import com.intellij.ui.JBColor;
import com.intellij.vcs.log.paint.ColorGenerator;
import java.awt.Color;
import java.util.Map;

public class SyncToOriginStatusColorGenerator implements ColorGenerator {

  public static final Color red = Color.decode("#FF0000");
  public static final Color orange = Color.decode("#FDA909");
  public static final Color orange_darker = Color.decode("#D68C00");
  /*
   * JBColor are pairs of colors for light and dark IDE theme.
   * Lhs colors names are their "general" description.
   * They may differ from rhs (which are actual and specific colors).
   */
  public static final JBColor RED = new JBColor(red, red);
  public static final JBColor ORANGE = new JBColor(orange_darker, orange);

  private static final Map<Integer, JBColor> gitMacheteColors =
      Map.of(
          Untracked.getId(), ORANGE,
          Ahead.getId(), RED,
          Behind.getId(), RED,
          Diverged.getId(), RED);

  @Override
  public Color getColor(int statusId) {
    return gitMacheteColors.getOrDefault(statusId, JBColor.GRAY);
  }
}
