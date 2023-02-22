package com.virtuslab.gitmachete.frontend.graph.impl.paint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import javax.swing.JTable;

import com.intellij.ui.JBColor;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.defs.Colors;
import com.virtuslab.gitmachete.frontend.graph.api.items.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainter;
import com.virtuslab.gitmachete.frontend.graph.api.paint.PaintParameters;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IEdgeRenderPart;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;

@RequiredArgsConstructor
public class GraphCellPainter implements IGraphCellPainter {

  private final JTable table;

  @UIEffect
  protected int getRowHeight() {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    val font = table.getFont();
    // The font (if missing) is being retrieved from parent. In the case of parent absence it could be null.
    if (font != null) {
      // The point is to scale the graph table content (text and graph) along with the font specified by settings.
      return Math.max(table.getFontMetrics(font).getHeight(), table.getRowHeight());
    } else {
      return table.getRowHeight();
    }
  }

  @UIEffect
  private BasicStroke getOrdinaryStroke() {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    return new BasicStroke(PaintParameters.getLineThickness(getRowHeight()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
  }

  @UIEffect
  private void paintUpLine(Graphics2D g2, Color color, int posInRow) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int x = nodeWidth * posInRow + nodeWidth / 2;
    int y1 = getRowHeight() / 2 - 1;
    int y2 = 0;
    paintLine(g2, color, x, y1, x, y2);
  }

  @UIEffect
  private void paintDownLine(Graphics2D g2, Color color, int posInRow) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int y2 = getRowHeight();
    int y1 = getRowHeight() / 2;
    int x = nodeWidth * posInRow + nodeWidth / 2;
    paintLine(g2, color, x, y1, x, y2);
  }

  @UIEffect
  private void paintRightLine(Graphics2D g2, Color color, int posInRow) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int x1 = nodeWidth * posInRow + nodeWidth / 2;
    int x2 = x1 + nodeWidth;
    int y = getRowHeight() / 2;
    paintLine(g2, color, x1, y, x2, y);
  }

  @UIEffect
  private void paintLine(Graphics2D g2, Color color, int x1, int y1, int x2, int y2) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    g2.setColor(color);
    g2.setStroke(getOrdinaryStroke());
    g2.drawLine(x1, y1, x2, y2);
  }

  @UIEffect
  private void paintCircle(Graphics2D g2, int position, Color color) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int x0 = nodeWidth * position + nodeWidth / 2;
    int y0 = getRowHeight() / 2;
    int r = PaintParameters.getCircleRadius(getRowHeight());
    Ellipse2D.Double circle = new Ellipse2D.Double(x0 - r + 0.5, y0 - r + 0.5, 2 * r, 2 * r);
    g2.setColor(color);
    g2.fill(circle);
  }

  private static final Map<GraphItemColor, JBColor> COLORS = HashMap.of(
      GraphItemColor.GRAY, Colors.GRAY,
      GraphItemColor.YELLOW, Colors.YELLOW,
      GraphItemColor.RED, Colors.RED,
      GraphItemColor.GREEN, Colors.GREEN);

  @UIEffect
  private Color getColor(IRenderPart renderPart) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    return COLORS.getOrElse(renderPart.getGraphItemColor(), Colors.TRANSPARENT);
  }

  @Override
  @UIEffect
  public void draw(Graphics2D g2, List<? extends IRenderPart> renderParts) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    for (IRenderPart renderPart : renderParts) {
      drawRenderPart(g2, renderPart);
    }
  }

  @UIEffect
  protected void drawRenderPart(Graphics2D g2, IRenderPart renderPart) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    if (renderPart.isNode()) {
      int posInRow = renderPart.getPositionInRow();
      paintCircle(g2, posInRow, getColor(renderPart));
    } else { // isEdge
      drawEdge(g2, getColor(renderPart), renderPart.asEdge());
    }
  }

  @UIEffect
  private void drawEdge(Graphics2D g2, Color color, IEdgeRenderPart edgeRenderPart) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    int posInRow = edgeRenderPart.getPositionInRow();

    if (edgeRenderPart.getType() == IEdgeRenderPart.Type.DOWN) {
      paintDownLine(g2, color, posInRow);
    } else if (edgeRenderPart.getType() == IEdgeRenderPart.Type.UP) {
      paintUpLine(g2, color, posInRow);
    } else if (edgeRenderPart.getType() == IEdgeRenderPart.Type.RIGHT) {
      paintRightLine(g2, color, posInRow);
    }
  }
}
