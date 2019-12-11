package com.virtuslab.gitmachete.ui.render;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleColoredRenderer;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.graph.EdgePrintElement;
import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.paint.GraphCellPainter;
import com.intellij.vcs.log.paint.PaintParameters;
import com.intellij.vcs.log.ui.render.LabelPainter;
import com.intellij.vcs.log.ui.render.TypeSafeTableCellRenderer;
import com.virtuslab.gitmachete.ui.table.GitMacheteGraphTable;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.swing.JTable;

/* todo: refactor */
public class GraphBranchCellRenderer extends TypeSafeTableCellRenderer<GraphBranchCell> {

  @Nonnull private final MyComponent myComponent;

  public GraphBranchCellRenderer(
      @Nonnull GraphCellPainter painter, @Nonnull GitMacheteGraphTable table) {
    myComponent = new MyComponent(painter, table);
  }

  @Override
  protected SimpleColoredComponent getTableCellRendererComponentImpl(
      @Nonnull JTable table,
      @Nonnull GraphBranchCell value,
      boolean isSelected,
      boolean hasFocus,
      int row,
      int column) {
    myComponent.customize(value, isSelected, hasFocus, row, column);
    return myComponent;
  }

  private static class MyComponent extends SimpleColoredRenderer {
    @Nonnull private final GitMacheteGraphTable myGraphTable;
    @Nonnull private final GraphCellPainter myPainter;

    @Nonnull
    GraphImage myGraphImage =
        new GraphImage(UIUtil.createImage(1, 1, BufferedImage.TYPE_INT_ARGB), 0);

    MyComponent(@Nonnull GraphCellPainter painter, @Nonnull GitMacheteGraphTable table) {
      myPainter = painter;
      myGraphTable = table;
    }

    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      Graphics2D g2d = (Graphics2D) g;
      // The image's origin (after the graphics translate is applied) is rounded by J2D with .5
      // coordinate ceil'd.
      // This doesn't correspond to how the rectangle's origin is rounded, with .5 floor'd. As the
      // result, there may be a gap
      // b/w the background's top and the image's top (depending on the row number and the graphics
      // translate). To avoid that,
      // the graphics y-translate is aligned to int with .5-floor-bias.
      AffineTransform origTx =
          PaintUtil.alignTxToInt(g2d, null, false, true, PaintUtil.RoundingMode.ROUND_FLOOR_BIAS);
      try {
        UIUtil.drawImage(g, myGraphImage.getImage(), 0, 0, null);
      } finally {
        if (origTx != null) g2d.setTransform(origTx);
      }
    }

    void customize(
        @Nonnull GraphBranchCell cell, boolean isSelected, boolean hasFocus, int row, int column) {
      clear();
      setPaintFocusBorder(false);
      acquireState(myGraphTable, isSelected, hasFocus, row, column);
      getCellState().updateRenderer(this);
      setBorder(null);

      myGraphImage = getGraphImage(cell.getPrintElements());

      append(""); // appendTextPadding wont work without this

      appendTextPadding(myGraphImage.getWidth() + LabelPainter.RIGHT_PADDING.get());
      appendText(cell, isSelected);
    }

    private void appendText(GraphBranchCell cell, boolean isSelected) {
      append(cell.getText());
      SpeedSearchUtil.applySpeedSearchHighlighting(myGraphTable, this, false, isSelected);
    }

    @Nonnull
    private GraphImage getGraphImage(@Nonnull Collection<? extends PrintElement> printElements) {
      double maxIndex = getMaxGraphElementIndex(printElements);
      BufferedImage image =
          UIUtil.createImage(
              myGraphTable.getGraphicsConfiguration(),
              (int) (PaintParameters.getNodeWidth(myGraphTable.getRowHeight()) * (maxIndex + 2)),
              myGraphTable.getRowHeight(),
              BufferedImage.TYPE_INT_ARGB,
              PaintUtil.RoundingMode.CEIL);
      Graphics2D g2 = image.createGraphics();
      myPainter.draw(g2, printElements);

      int width = (int) (maxIndex * PaintParameters.getNodeWidth(myGraphTable.getRowHeight()));
      return new GraphImage(image, width);
    }

    private double getMaxGraphElementIndex(
        @Nonnull Collection<? extends PrintElement> printElements) {
      double maxIndex = 0;
      for (PrintElement printElement : printElements) {
        maxIndex = Math.max(maxIndex, printElement.getPositionInCurrentRow());
        if (printElement instanceof EdgePrintElement) {
          maxIndex =
              Math.max(
                  maxIndex,
                  (printElement.getPositionInCurrentRow()
                          + ((EdgePrintElement) printElement).getPositionInOtherRow())
                      / 2.0);
        }
      }
      maxIndex++;

      return maxIndex;
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
      return myGraphTable.getFontMetrics(font);
    }
  }

  private static class GraphImage {
    private final int myWidth;
    @Nonnull private final Image myImage;

    GraphImage(@Nonnull Image image, int width) {
      myImage = image;
      myWidth = width;
    }

    @Nonnull
    Image getImage() {
      return myImage;
    }

    /**
     * Returns the "interesting" width of the painted image, i.e. the width which the text in the
     * table should be offset by. <br>
     * It can be smaller than the width of {@link #getImage() the image}, because we allow the text
     * to cover part of the graph (some diagonal edges, etc.)
     */
    int getWidth() {
      return myWidth;
    }
  }
}
