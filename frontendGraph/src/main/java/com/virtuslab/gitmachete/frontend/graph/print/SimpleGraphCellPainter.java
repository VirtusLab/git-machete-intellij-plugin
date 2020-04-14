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
import com.intellij.vcs.log.paint.GraphCellPainter;
import com.intellij.vcs.log.paint.PaintParameters;
import com.intellij.vcs.log.paint.PositionUtil;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

@RequiredArgsConstructor
public class SimpleGraphCellPainter implements GraphCellPainter {

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
  private void paintUpLine(Graphics2D g2, Color color, int from, int to) {
    // paint vertical lines normal size
    // paint non-vertical lines twice the size to make them dock with each other well
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    if (from == to) {
      int x = nodeWidth * from + nodeWidth / 2;
      int y1 = getRowHeight() / 2 - 1;
      int y2 = 0;
      paintLine(g2, color, x, y1, x, y2);
    } else {
      int x1 = nodeWidth * from + nodeWidth / 2;
      int y1 = getRowHeight() / 2;
      int x2 = nodeWidth * to + nodeWidth / 2;
      int y2 = -getRowHeight() / 2;
      paintLine(g2, color, x1, y1, x2, y2);
    }
  }

  @UIEffect
  private void paintDownLine(Graphics2D g2, Color color, int from, int to) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    if (from == to) {
      int y2 = getRowHeight();
      int y1 = getRowHeight() / 2;
      int x = nodeWidth * from + nodeWidth / 2;
      paintLine(g2, color, x, y1, x, y2);
    } else {
      int x1 = nodeWidth * from + nodeWidth / 2;
      int y1 = getRowHeight() / 2;
      int x2 = nodeWidth * to + nodeWidth / 2;
      int y2 = getRowHeight() + getRowHeight() / 2;
      paintLine(g2, color, x1, y1, x2, y2);
    }
  }

  @UIEffect
  private void paintRightLine(Graphics2D g2, Color color, int from, int to) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int x1 = nodeWidth * from + nodeWidth / 2;
    int y = getRowHeight() / 2;
    int x2 = nodeWidth * to + nodeWidth / 2;
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
  @Override
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
      int position = printElement.getPositionInCurrentRow();
      paintCircle(g2, position, getColor(printElement));
    }
  }

  @UIEffect
  private void printVerticalEdge(Graphics2D g2, Color color, EdgePrintElement edgePrintElement) {
    int from = edgePrintElement.getPositionInCurrentRow();
    int to = edgePrintElement.getPositionInOtherRow();

    if (edgePrintElement.getType() == EdgePrintElement.Type.DOWN) {
      paintDownLine(g2, color, from, to);
    } else if (edgePrintElement.getType() == EdgePrintElement.Type.UP) {
      paintUpLine(g2, color, from, to);
    }
  }

  @UIEffect
  private void printRightEdge(Graphics2D g2, Color color, RightEdgePrintElement rightEdgePrintElement) {
    int from = rightEdgePrintElement.getPositionInCurrentRow();
    paintRightLine(g2, color, from, from + 1);
  }

  @UIEffect
  @Override
  @Nullable
  public PrintElement getElementUnderCursor(Collection<? extends PrintElement> printElements, int x, int y) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    for (PrintElement printElement : printElements) {
      if (printElement instanceof NodePrintElement) {
        int circleRadius = PaintParameters.getCircleRadius(getRowHeight());
        if (PositionUtil.overNode(printElement.getPositionInCurrentRow(), x, y, getRowHeight(), nodeWidth,
            circleRadius)) {
          return printElement;
        }
      }
    }

    for (PrintElement printElement : printElements) {
      if (printElement instanceof EdgePrintElement) {
        EdgePrintElement edgePrintElement = (EdgePrintElement) printElement;
        float lineThickness = PaintParameters.getLineThickness(getRowHeight());
        if (edgePrintElement.getType() == EdgePrintElement.Type.DOWN) {
          if (PositionUtil
              .overDownEdge(edgePrintElement.getPositionInCurrentRow(), edgePrintElement.getPositionInOtherRow(), x, y,
                  getRowHeight(),
                  nodeWidth, lineThickness)) {
            return printElement;
          }
        } else {
          if (PositionUtil
              .overUpEdge(edgePrintElement.getPositionInOtherRow(), edgePrintElement.getPositionInCurrentRow(), x, y,
                  getRowHeight(),
                  nodeWidth, lineThickness)) {
            return printElement;
          }
        }
      }
    }
    return null;
  }
}
