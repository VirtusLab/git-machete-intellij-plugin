package com.virtuslab.gitmachete.frontend.ui.impl.cell;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JTable;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleColoredRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.ui.render.LabelPainter;
import com.intellij.vcs.log.ui.render.TypeSafeTableCellRenderer;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.graph.api.coloring.SyncToRemoteStatusToTextColorMapper;
import com.virtuslab.gitmachete.frontend.graph.api.items.IBranchItem;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.labeling.SyncToRemoteStatusLabelGenerator;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainter;
import com.virtuslab.gitmachete.frontend.graph.api.paint.PaintParameters;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;
import com.virtuslab.gitmachete.frontend.ui.impl.table.GitMacheteGraphTable;

@UI
public class BranchOrCommitCellRenderer extends TypeSafeTableCellRenderer<BranchOrCommitCell> {
  private static final String CELL_TEXT_FRAGMENTS_SPACING = "   ";

  private final MyComponent myComponent;

  public BranchOrCommitCellRenderer(GitMacheteGraphTable table, IGraphCellPainter painter) {
    myComponent = new MyComponent(table, painter);
  }

  @Override
  protected SimpleColoredComponent getTableCellRendererComponentImpl(
      JTable table,
      BranchOrCommitCell value,
      boolean isSelected,
      boolean hasFocus,
      int row,
      int column) {
    myComponent.customize(value, isSelected, hasFocus, row, column);
    return myComponent;
  }

  private static class MyComponent extends SimpleColoredRenderer {
    private final GitMacheteGraphTable graphTable;
    private final IGraphCellPainter painter;

    GraphImage graphImage = new GraphImage(ImageUtil.createImage(1, 1, BufferedImage.TYPE_INT_ARGB), 0);

    @UIEffect
    MyComponent(GitMacheteGraphTable graphTable, IGraphCellPainter painter) {
      this.graphTable = graphTable;
      this.painter = painter;
    }

    @Override
    @UIEffect
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
      AffineTransform origTx = PaintUtil.alignTxToInt(/* graphics2d */ g2d, /* offset */ null, /* alignX */ false,
          /* alignY */ true, PaintUtil.RoundingMode.ROUND_FLOOR_BIAS);
      UIUtil.drawImage(g, graphImage.getImage(), 0, 0, null);
      if (origTx != null) {
        g2d.setTransform(origTx);
      }
    }

    @UIEffect
    void customize(BranchOrCommitCell cell, boolean isSelected, boolean hasFocus, int row, int column) {
      clear();
      setPaintFocusBorder(false);
      acquireState(graphTable, isSelected, hasFocus, row, column);
      getCellState().updateRenderer(this);
      setBorder(null);

      IGraphItem graphItem = cell.getGraphItem();

      int maxGraphNodePositionInRow = getMaxGraphNodePositionInRow(graphItem);

      if (graphItem.hasBulletPoint()) {
        graphImage = getGraphImage(cell.getRenderParts(), maxGraphNodePositionInRow);
      } else {
        graphImage = getGraphImage(cell.getRenderParts().toStream().filter(e -> !e.isNode())
            .collect(List.collector()), maxGraphNodePositionInRow);
      }

      append(""); // appendTextPadding won't work without this

      int textPadding = calculateTextPadding(maxGraphNodePositionInRow);
      appendTextPadding(textPadding);

      SimpleTextAttributes attributes = graphItem.getAttributes();
      append(cell.getText(), attributes);

      if (graphItem.isBranchItem()) {
        IBranchItem branchItem = graphItem.asBranchItem();
        IGitMacheteBranch branch = branchItem.getBranch();

        Option<String> customAnnotation = branch.getCustomAnnotation();
        if (customAnnotation.isDefined()) {
          append(CELL_TEXT_FRAGMENTS_SPACING + customAnnotation.get(), SimpleTextAttributes.GRAY_ATTRIBUTES);
        }

        Option<String> statusHookOutput = branch.getStatusHookOutput();
        if (statusHookOutput.isDefined()) {
          append("   " + statusHookOutput.get(), SimpleTextAttributes.GRAY_ATTRIBUTES);
        }

        SyncToRemoteStatus syncToRemoteStatus = branchItem.getSyncToRemoteStatus();
        if (syncToRemoteStatus.getRelation() != SyncToRemoteStatus.Relation.InSyncToRemote) {
          SimpleTextAttributes textAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
              SyncToRemoteStatusToTextColorMapper.getColor(syncToRemoteStatus.getRelation()));
          String remoteStatusLabel = SyncToRemoteStatusLabelGenerator.getLabel(syncToRemoteStatus.getRelation(),
              syncToRemoteStatus.getRemoteName());
          append("  (" + remoteStatusLabel + ")", textAttributes);
        }
      }
    }

    @UIEffect
    @Positive
    private int calculateTextPadding(@NonNegative int maxPosition) {
      int width = (maxPosition + 1) * PaintParameters.getNodeWidth(graphTable.getRowHeight());
      int padding = width + LabelPainter.RIGHT_PADDING.get();
      // Our assumption here comes from the fact that we expect positive row height of graph table,
      // hence non-negative width AND positive right padding from `LabelPainter.RIGHT_PADDING`.
      assert padding > 0 : "Padding is not greater than 0";
      return padding;
    }

    @NonNegative
    private int getMaxGraphNodePositionInRow(IGraphItem graphItem) {
      // If item is a child (non root) branch, then the text must be shifted right to make place
      // for the corresponding the right edge to the left.
      // If item is a commit, then the text must be shifted right to keep it horizontally aligned
      // with the corresponding branch item.
      boolean isRootBranch = graphItem.isBranchItem() && graphItem.asBranchItem().getBranch().isRootBranch();
      return graphItem.getIndentLevel() + (isRootBranch ? 0 : 1);
    }

    @UIEffect
    private GraphImage getGraphImage(List<? extends IRenderPart> renderParts,
        @NonNegative int maxGraphNodePositionInRow) {
      BufferedImage image = UIUtil.createImage(graphTable.getGraphicsConfiguration(),
          /* width */ PaintParameters.getNodeWidth(graphTable.getRowHeight()) * (maxGraphNodePositionInRow + 2),
          /* height */ graphTable.getRowHeight(),
          BufferedImage.TYPE_INT_ARGB,
          PaintUtil.RoundingMode.CEIL);
      Graphics2D g2 = image.createGraphics();
      painter.draw(g2, renderParts);

      int width = maxGraphNodePositionInRow * PaintParameters.getNodeWidth(graphTable.getRowHeight());
      return new GraphImage(image, width);
    }

    @Override
    @UIEffect
    public FontMetrics getFontMetrics(Font font) {
      return graphTable.getFontMetrics(font);
    }
  }

  @AllArgsConstructor
  @Getter
  private static class GraphImage {
    private final Image image;
    private final int width;
  }
}
