package com.virtuslab.gitmachete.graph;

import java.util.Arrays;

public enum GraphEdgeColor {
  TRANSPARENT(0),
  GRAY(1),
  YELLOW(2),
  RED(3),
  GREEN(4);

  private final int id;

  GraphEdgeColor(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public static GraphEdgeColor getById(int id) {
    return Arrays.stream(values()).filter(e -> e.getId() == id).findFirst().orElse(TRANSPARENT);
  }
}
