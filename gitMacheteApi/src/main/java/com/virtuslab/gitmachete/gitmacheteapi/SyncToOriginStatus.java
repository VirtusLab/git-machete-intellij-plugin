package com.virtuslab.gitmachete.gitmacheteapi;

import java.awt.Color;

public enum SyncToOriginStatus {
  Untracked("Untracked", new Color(255, 150, 0)),
  Ahead("Ahead of origin", new Color(255, 0, 0)),
  Behind("Behind of origin", new Color(255, 0, 0)),
  Diverged("Diverged from origin", new Color(255, 0, 0)),
  InSync("", Color.GREEN);

  private final String description;
  private final Color color;

  private SyncToOriginStatus(String description, Color color) {
    this.description = description;
    this.color = color;
  }

  public String getDescription() {
    return description;
  }

  public Color getColor() {
    return color;
  }
}
