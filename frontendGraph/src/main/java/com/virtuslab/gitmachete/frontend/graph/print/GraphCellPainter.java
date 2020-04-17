package com.virtuslab.gitmachete.frontend.graph.print;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.Collection;

import com.intellij.vcs.log.graph.EdgePrintElement;
import com.intellij.vcs.log.graph.NodePrintElement;
import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.paint.ColorGenerator;
import com.intellij.vcs.log.paint.PaintParameters;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.guieffect.qual.UIEffect;

@RequiredArgsConstructor
public class GraphCellPainter {

  private final ColorGenerator myColorGenerator;

  @UIEffect
  protected int getRowHeight() {
    return PaintParameters.ROW_HEIGHT;
  }

  @UIEffect
  private BasicStroke getOrdinaryStroke() {
    return new BasicStroke(PaintParameters.getLineThickness(getRowHeight()), BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_BEVEL);
  }

  @UIEffect
  private void paintUpLine(Graphics2D g2, Color color, int posInRow) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int x = nodeWidth * posInRow + nodeWidth / 2;
    int y1 = getRowHeight() / 2 - 1;
    int y2 = 0;
    paintLine(g2, color, x, y1, x, y2);
  }

  @UIEffect
  private void paintDownLine(Graphics2D g2, Color color, int posInRow) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int y2 = getRowHeight();
    int y1 = getRowHeight() / 2;
    int x = nodeWidth * posInRow + nodeWidth / 2;
    paintLine(g2, color, x, y1, x, y2);
  }

  @UIEffect
  private void paintRightLine(Graphics2D g2, Color color, int posInRow) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int x1 = nodeWidth * posInRow + nodeWidth / 2;
    int x2 = x1 + nodeWidth;
    int y = getRowHeight() / 2;
    paintLine(g2, color, x1, y, x2, y);
  }

  @UIEffect
  private void paintLine(Graphics2D g2, Color color, int x1, int y1, int x2, int y2) {
    g2.setColor(color);
    g2.setStroke(getOrdinaryStroke());
    g2.drawLine(x1, y1, x2, y2);
  }

  @UIEffect
  private void paintCircle(Graphics2D g2, int position, Color color) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int circleRadius = PaintParameters.getCircleRadius(getRowHeight());

    int x0 = nodeWidth * position + nodeWidth / 2;
    int y0 = getRowHeight() / 2;
    int r = circleRadius;
    Ellipse2D.Double circle = new Ellipse2D.Double(x0 - r + 0.5, y0 - r + 0.5, 2 * r, 2 * r);
    g2.setColor(color);
    g2.fill(circle);
  }

  @UIEffect
  private Color getColor(PrintElement printElement) {
    return myColorGenerator.getColor(printElement.getColorId());
  }

  @UIEffect
  public void draw(Graphics2D g2, Collection<? extends PrintElement> printElements) {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    for (PrintElement printElement : printElements) {
      drawElement(g2, printElement);
    }
  }

  @UIEffect
  protected void drawElement(Graphics2D g2, PrintElement printElement) {
    if (printElement instanceof EdgePrintElement) {
      printVerticalEdge(g2, getColor(printElement), (EdgePrintElement) printElement);
    }

    if (printElement instanceof RightEdgePrintElement) {
      printRightEdge(g2, getColor(printElement), (RightEdgePrintElement) printElement);
    }

    if (printElement instanceof NodePrintElement) {
      int posInRow = printElement.getPositionInCurrentRow();
      paintCircle(g2, posInRow, getColor(printElement));
    }
  }

  @UIEffect
  private void printVerticalEdge(Graphics2D g2, Color color, EdgePrintElement edgePrintElement) {
    int posInRow = edgePrintElement.getPositionInCurrentRow();
    assert posInRow == edgePrintElement.getPositionInOtherRow();

    if (edgePrintElement.getType() == EdgePrintElement.Type.DOWN) {
      paintDownLine(g2, color, posInRow);
    } else if (edgePrintElement.getType() == EdgePrintElement.Type.UP) {
      paintUpLine(g2, color, posInRow);
    }
  }

  @UIEffect
  private void printRightEdge(Graphics2D g2, Color color, RightEdgePrintElement rightEdgePrintElement) {
    int posInRow = rightEdgePrintElement.getPositionInCurrentRow();
    paintRightLine(g2, color, posInRow);
  }
}
