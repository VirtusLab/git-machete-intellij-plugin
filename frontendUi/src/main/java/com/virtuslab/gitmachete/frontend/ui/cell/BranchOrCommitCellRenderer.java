package com.virtuslab.gitmachete.frontend.ui.cell;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JTable;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleColoredRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.graph.NodePrintElement;
import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.paint.PaintParameters;
import com.intellij.vcs.log.ui.render.LabelPainter;
import com.intellij.vcs.log.ui.render.TypeSafeTableCellRenderer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.graph.coloring.SyncToRemoteStatusToTextColorMapper;
import com.virtuslab.gitmachete.frontend.graph.labeling.SyncToRemoteStatusLabelGenerator;
import com.virtuslab.gitmachete.frontend.graph.nodes.BranchNode;
import com.virtuslab.gitmachete.frontend.graph.nodes.IGraphNode;
import com.virtuslab.gitmachete.frontend.graph.print.GraphCellPainter;
import com.virtuslab.gitmachete.frontend.ui.table.GitMacheteGraphTable;

@UI
public class BranchOrCommitCellRenderer extends TypeSafeTableCellRenderer<BranchOrCommitCell> {

  private final MyComponent myComponent;

  public BranchOrCommitCellRenderer(GitMacheteGraphTable table, GraphCellPainter painter) {
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
    private final GraphCellPainter painter;

    // Note: using deprecated `UIUtil.createImage` instead of `ImageUtil.createImage` to maintain compatibility with
    // IntelliJ platform 2019.2
    GraphImage graphImage = new GraphImage(UIUtil.createImage(1, 1, BufferedImage.TYPE_INT_ARGB), 0);

    @UIEffect
    MyComponent(GitMacheteGraphTable graphTable, GraphCellPainter painter) {
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

      IGraphNode node = cell.getGraphNode();

      int maxGraphNodePositionInRow = getMaxGraphNodePositionInRow(node);

      if (node.hasBulletPoint()) {
        graphImage = getGraphImage(cell.getPrintElements(), maxGraphNodePositionInRow);
      } else {
        graphImage = getGraphImage(cell.getPrintElements().stream().filter(e -> !(e instanceof NodePrintElement))
            .collect(Collectors.toList()), maxGraphNodePositionInRow);
      }

      append(""); // appendTextPadding won't work without this

      int textPadding = calculateTextPadding(maxGraphNodePositionInRow);
      appendTextPadding(textPadding);

      SimpleTextAttributes attributes = node.getAttributes();
      append(cell.getText(), attributes);

      if (node instanceof BranchNode) {
        BaseGitMacheteBranch branch = ((BranchNode) node).getBranch();
        Optional<String> customAnnotation = branch.getCustomAnnotation();
        if (customAnnotation.isPresent()) {
          append("   " + customAnnotation.get(), SimpleTextAttributes.GRAY_ATTRIBUTES);
        }

        ISyncToRemoteStatus syncToRemoteStatus;

        syncToRemoteStatus = ((BranchNode) node).getSyncToRemoteStatus();
        if (syncToRemoteStatus.getRelation() != ISyncToRemoteStatus.Relation.InSync) {
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
      assert padding > 0;
      return padding;
    }

    @NonNegative
    private int getMaxGraphNodePositionInRow(IGraphNode node) {
      // If node is a child (non root) branch, then the text must be shifted right to make place
      // for the corresponding the right edge to the left.
      // If node is a commit, then the text must be shifted right to keep it horizontally aligned
      // with the corresponding branch node.
      boolean isRootBranch = node.isBranch() && ((BranchNode) node).getBranch().isRootBranch();
      return node.getIndentLevel() + (isRootBranch ? 0 : 1);
    }

    @UIEffect
    private GraphImage getGraphImage(Collection<? extends PrintElement> printElements,
        @NonNegative int maxGraphNodePositionInRow) {
      BufferedImage image = UIUtil.createImage(graphTable.getGraphicsConfiguration(),
          /* width */ PaintParameters.getNodeWidth(graphTable.getRowHeight()) * (maxGraphNodePositionInRow + 2),
          /* height */ graphTable.getRowHeight(),
          BufferedImage.TYPE_INT_ARGB,
          PaintUtil.RoundingMode.CEIL);
      Graphics2D g2 = image.createGraphics();
      painter.draw(g2, printElements);

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
