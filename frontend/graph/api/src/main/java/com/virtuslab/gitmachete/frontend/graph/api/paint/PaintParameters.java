package com.virtuslab.gitmachete.frontend.graph.api.paint;

public final class PaintParameters {

  private PaintParameters() {}

  private static final int CIRCLE_RADIUS = 4;
  private static final float THICK_LINE = 1.5f;
  private static final int WIDTH_NODE = 15;

  public static final int ROW_HEIGHT = 22;

  public static int getNodeWidth(int rowHeight) {
    return WIDTH_NODE * rowHeight / ROW_HEIGHT;
  }

  public static float getLineThickness(int rowHeight) {
    return THICK_LINE * rowHeight / ROW_HEIGHT;
  }

  public static int getCircleRadius(int rowHeight) {
    return CIRCLE_RADIUS * rowHeight / ROW_HEIGHT;
  }
}
