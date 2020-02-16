package com.virtuslab.gitmachete.ui.cell;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleColoredRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.ui.ImageUtil;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class BranchOrCommitCellRenderer extends TypeSafeTableCellRenderer<BranchOrCommitCell> {

  @Nonnull private final MyComponent myComponent;

  public BranchOrCommitCellRenderer(
      @Nonnull GitMacheteGraphTable table, @Nonnull GraphCellPainter painter) {
    myComponent = new MyComponent(table, painter);
  }

  @Override
  protected SimpleColoredComponent getTableCellRendererComponentImpl(
      @Nonnull JTable table,
      @Nonnull BranchOrCommitCell value,
      boolean isSelected,
      boolean hasFocus,
      int row,
      int column) {
    myComponent.customize(value, isSelected, hasFocus, row, column);
    return myComponent;
  }

  @RequiredArgsConstructor
  private static class MyComponent extends SimpleColoredRenderer {
    @Nonnull private final GitMacheteGraphTable graphTable;
    @Nonnull private final GraphCellPainter painter;

    @Nonnull
    GraphImage graphImage =
        new GraphImage(ImageUtil.createImage(1, 1, BufferedImage.TYPE_INT_ARGB), 0);

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
        UIUtil.drawImage(g, graphImage.getImage(), 0, 0, null);
      } finally {
        if (origTx != null) g2d.setTransform(origTx);
      }
    }

    void customize(
        @Nonnull BranchOrCommitCell cell,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {
      clear();
      setPaintFocusBorder(false);
      acquireState(graphTable, isSelected, hasFocus, row, column);
      getCellState().updateRenderer(this);
      setBorder(null);

      graphImage = getGraphImage(cell.getPrintElements());

      append(""); // appendTextPadding wont work without this

      appendTextPadding(graphImage.getWidth() + LabelPainter.RIGHT_PADDING.get());
      SimpleTextAttributes attributes = cell.getElement().getAttributes();
      appendText(cell, attributes, isSelected);
    }

    private void appendText(
        BranchOrCommitCell cell, SimpleTextAttributes attributes, boolean isSelected) {
      append(cell.getText(), attributes);
      SpeedSearchUtil.applySpeedSearchHighlighting(graphTable, this, false, isSelected);
    }

    @Nonnull
    private GraphImage getGraphImage(@Nonnull Collection<? extends PrintElement> printElements) {
      double maxIndex = getMaxGraphElementIndex(printElements);
      BufferedImage image =
          UIUtil.createImage(
              graphTable.getGraphicsConfiguration(),
              (int) (PaintParameters.getNodeWidth(graphTable.getRowHeight()) * (maxIndex + 2)),
              graphTable.getRowHeight(),
              BufferedImage.TYPE_INT_ARGB,
              PaintUtil.RoundingMode.CEIL);
      Graphics2D g2 = image.createGraphics();
      painter.draw(g2, printElements);

      int width = (int) (maxIndex * PaintParameters.getNodeWidth(graphTable.getRowHeight()));
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
      return graphTable.getFontMetrics(font);
    }
  }

  @AllArgsConstructor
  @Getter
  private static class GraphImage {
    @Nonnull private final Image image;
    private final int width;
  }
}
