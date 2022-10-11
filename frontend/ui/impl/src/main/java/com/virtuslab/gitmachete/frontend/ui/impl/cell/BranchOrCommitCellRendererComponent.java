package com.virtuslab.gitmachete.frontend.ui.impl.cell;

import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN;
import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.InSync;
import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.InSyncButForkPointOff;
import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.MergedToParent;
import static com.virtuslab.gitmachete.backend.api.SyncToParentStatus.OutOfSync;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.InSyncToRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.NoRemotes;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Untracked;
import static com.virtuslab.gitmachete.frontend.defs.Colors.ORANGE;
import static com.virtuslab.gitmachete.frontend.defs.Colors.RED;
import static com.virtuslab.gitmachete.frontend.defs.Colors.TRANSPARENT;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.isIn;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.ui.render.LabelPainter;
import io.vavr.collection.List;
import lombok.Data;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.INonRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.IRootManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.OngoingRepositoryOperationType;
import com.virtuslab.gitmachete.backend.api.RelationToRemote;
import com.virtuslab.gitmachete.frontend.defs.Colors;
import com.virtuslab.gitmachete.frontend.graph.api.items.IBranchItem;
import com.virtuslab.gitmachete.frontend.graph.api.items.ICommitItem;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainterFactory;
import com.virtuslab.gitmachete.frontend.graph.api.paint.PaintParameters;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.impl.table.IGitMacheteRepositorySnapshotProvider;

@ExtensionMethod(GitMacheteBundle.class)
public final class BranchOrCommitCellRendererComponent extends SimpleColoredRenderer {
  private static final String CELL_TEXT_FRAGMENTS_SPACING = "   ";
  private static final String HEAVY_WIDE_HEADED_RIGHTWARDS_ARROW = "\u2794";

  private static final IGraphCellPainterFactory graphCellPainterFactoryInstance = RuntimeBinding
      .instantiateSoleImplementingClass(IGraphCellPainterFactory.class);

  private final JTable graphTable;
  private final BufferedImage graphImage;
  private final MyTableCellRenderer myTableCellRenderer;

  @UIEffect
  @SuppressWarnings("keyfor:assignment")
  public BranchOrCommitCellRendererComponent(
      JTable table,
      Object value,
      boolean isSelected,
      boolean hasFocus,
      int row,
      int column,
      boolean shouldDisplayActionToolTips) {

    this.graphTable = table;

    assert table instanceof IGitMacheteRepositorySnapshotProvider
        : "Table variable is not instance of ${IGitMacheteRepositorySnapshotProvider.class.getSimpleName()}";
    val gitMacheteRepositorySnapshot = ((IGitMacheteRepositorySnapshotProvider) graphTable).getGitMacheteRepositorySnapshot();

    assert value instanceof BranchOrCommitCell : "value is not an instance of " + BranchOrCommitCell.class.getSimpleName();
    val cell = (BranchOrCommitCell) value;

    IGraphItem graphItem = cell.getGraphItem();
    int maxGraphNodePositionInRow = getMaxGraphNodePositionInRow(graphItem);
    List<? extends IRenderPart> renderParts;
    if (graphItem.hasBulletPoint()) {
      renderParts = cell.getRenderParts();
    } else {
      renderParts = cell.getRenderParts().filter(e -> !e.isNode());
    }
    this.graphImage = getGraphImage(graphTable, maxGraphNodePositionInRow);
    Graphics2D g2 = graphImage.createGraphics();
    val graphCellPainter = graphCellPainterFactoryInstance.create(table);
    graphCellPainter.draw(g2, renderParts);

    this.myTableCellRenderer = new MyTableCellRenderer();

    // `this` is @Initialized at this point (since the class is final).

    clear();
    setPaintFocusBorder(false);
    acquireState(table, isSelected, hasFocus, row, column);
    getCellState().updateRenderer(this);
    setBorder(null);

    applyHighlighters(/* rendererComponent */ this, row, column, hasFocus, isSelected);

    append(""); // appendTextPadding won't work without this

    int textPadding = calculateTextPadding(graphTable, maxGraphNodePositionInRow);
    appendTextPadding(textPadding);

    if (gitMacheteRepositorySnapshot != null && graphItem.isBranchItem()) {
      val repositoryOperation = gitMacheteRepositorySnapshot.getOngoingRepositoryOperation();

      if (repositoryOperation.getOperationType() != OngoingRepositoryOperationType.NO_OPERATION) {
        SimpleTextAttributes attributes = SimpleTextAttributes.ERROR_ATTRIBUTES;
        val maybeOperationsBaseBranchName = repositoryOperation.getBaseBranchName();

        if (maybeOperationsBaseBranchName != null
            && Objects.equals(maybeOperationsBaseBranchName, graphItem.getValue())) {
          val ongoingOperationName = Match(repositoryOperation.getOperationType()).of(
              Case($(OngoingRepositoryOperationType.BISECTING),
                  getString("string.GitMachete.BranchOrCommitCellRendererComponent.ongoing-operation.bisecting")),
              Case($(OngoingRepositoryOperationType.REBASING),
                  getString("string.GitMachete.BranchOrCommitCellRendererComponent.ongoing-operation.rebasing")),
              Case($(), ""));

          append(ongoingOperationName + CELL_TEXT_FRAGMENTS_SPACING, attributes);

        } else if (graphItem.asBranchItem().isCurrentBranch()) {
          val ongoingOperationName = Match(repositoryOperation.getOperationType()).of(
              Case($(OngoingRepositoryOperationType.CHERRY_PICKING),
                  getString("string.GitMachete.BranchOrCommitCellRendererComponent.ongoing-operation.cherry-picking")),
              Case($(OngoingRepositoryOperationType.MERGING),
                  getString("string.GitMachete.BranchOrCommitCellRendererComponent.ongoing-operation.merging")),
              Case($(OngoingRepositoryOperationType.REVERTING),
                  getString("string.GitMachete.BranchOrCommitCellRendererComponent.ongoing-operation.reverting")),
              Case($(OngoingRepositoryOperationType.APPLYING),
                  getString("string.GitMachete.BranchOrCommitCellRendererComponent.ongoing-operation.applying")),
              Case($(), ""));

          append(ongoingOperationName + CELL_TEXT_FRAGMENTS_SPACING, attributes);
        }
      }
    }

    SimpleTextAttributes attributes = graphItem.getAttributes();
    append(cell.getText(), attributes);

    if (graphItem.isBranchItem()) {
      IBranchItem branchItem = graphItem.asBranchItem();
      IManagedBranchSnapshot branch = branchItem.getBranch();

      if (shouldDisplayActionToolTips) {
        setBranchToolTipText(branch);
      }

      String customAnnotation = branch.getCustomAnnotation();
      if (customAnnotation != null) {
        append(CELL_TEXT_FRAGMENTS_SPACING + customAnnotation, GRAY_ATTRIBUTES);
      }

      String statusHookOutput = branch.getStatusHookOutput();
      if (statusHookOutput != null) {
        append(CELL_TEXT_FRAGMENTS_SPACING + statusHookOutput, GRAY_ATTRIBUTES);
      }

      RelationToRemote relationToRemote = branchItem.getRelationToRemote();
      val textAttributes = new SimpleTextAttributes(STYLE_PLAIN, getColor(relationToRemote));
      String relationToRemoteLabel = getRelationToRemoteBasedLabel(relationToRemote);
      append(" " + relationToRemoteLabel, textAttributes);
    } else {
      ICommitItem commitItem = graphItem.asCommitItem();
      INonRootManagedBranchSnapshot containingBranch = commitItem.getContainingBranch();
      val forkPoint = containingBranch.getForkPoint();

      if (commitItem.getCommit().equals(forkPoint)) {
        append(
            " ${HEAVY_WIDE_HEADED_RIGHTWARDS_ARROW} "
                + getString("string.GitMachete.BranchOrCommitCellRendererComponent.inferred-fork-point.fork-point") + " ",
            new SimpleTextAttributes(STYLE_PLAIN, Colors.RED));
        append(getString("string.GitMachete.BranchOrCommitCellRendererComponent.inferred-fork-point.commit") + " ",
            REGULAR_ATTRIBUTES);
        append(forkPoint.getShortHash(), REGULAR_BOLD_ATTRIBUTES);
        append(" " + getString("string.GitMachete.BranchOrCommitCellRendererComponent.inferred-fork-point.found-in-reflog")
            + " ", REGULAR_ATTRIBUTES);
        append(forkPoint.getUniqueBranchesContainingInReflog()
            .map(b -> b.getName()).sorted().mkString(", "), REGULAR_BOLD_ATTRIBUTES);
      }
    }
  }

  @Override
  @UIEffect
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics2D g2d = (Graphics2D) g;
    // The image's origin (after the graphics translate is applied) is rounded by J2D with .5 coordinate ceil'd.
    // This doesn't correspond to how the rectangle's origin is rounded, with .5 floor'd. As the result, there may be a gap
    // b/w the background's top and the image's top (depending on the row number and the graphics translate). To avoid that,
    // the graphics y-translate is aligned to int with .5-floor-bias.
    AffineTransform origTx = PaintUtil.alignTxToInt(/* graphics2d */ g2d, /* offset */ null, /* alignX */ false,
        /* alignY */ true, PaintUtil.RoundingMode.ROUND_FLOOR_BIAS);
    UIUtil.drawImage(g, graphImage, 0, 0, null);
    if (origTx != null) {
      g2d.setTransform(origTx);
    }
  }

  @Override
  @UIEffect
  public FontMetrics getFontMetrics(Font font) {
    return graphTable.getFontMetrics(font);
  }

  private static @NonNegative int getMaxGraphNodePositionInRow(IGraphItem graphItem) {
    // If item is a child (non root) branch, then the text must be shifted right to make place
    // for the corresponding the right edge to the left.
    // If item is a commit, then the text must be shifted right to keep it horizontally aligned
    // with the corresponding branch item.
    boolean isRootBranch = graphItem.isBranchItem() && graphItem.asBranchItem().getBranch().isRoot();
    return graphItem.getIndentLevel() + (isRootBranch ? 0 : 1);
  }

  @UIEffect
  @SuppressWarnings("nullness:argument") // for GraphicsConfiguration param
  private static BufferedImage getGraphImage(JTable table, @NonNegative int maxGraphNodePositionInRow) {
    return UIUtil.createImage(table.getGraphicsConfiguration(),
        /* width */ PaintParameters.getNodeWidth(table.getRowHeight()) * (maxGraphNodePositionInRow + 2),
        /* height */ table.getRowHeight(),
        BufferedImage.TYPE_INT_ARGB,
        PaintUtil.RoundingMode.CEIL);
  }

  @UIEffect
  private static @Positive int calculateTextPadding(JTable table, @NonNegative int maxPosition) {
    int width = (maxPosition + 1) * PaintParameters.getNodeWidth(table.getRowHeight());
    int padding = width + LabelPainter.RIGHT_PADDING.get();
    // Our assumption here comes from the fact that we expect positive row height of graph table,
    // hence non-negative width AND positive right padding from `LabelPainter.RIGHT_PADDING`.
    assert padding > 0 : "Padding is not greater than 0";
    return padding;
  }

  private static JBColor getColor(RelationToRemote relation) {
    return Match(relation.getSyncToRemoteStatus()).of(
        Case($(isIn(NoRemotes, InSyncToRemote)), TRANSPARENT),
        Case($(Untracked), ORANGE),
        Case($(isIn(AheadOfRemote, BehindRemote, DivergedFromAndNewerThanRemote, DivergedFromAndOlderThanRemote)), RED));
  }

  private static String getRelationToRemoteBasedLabel(RelationToRemote relation) {
    val maybeRemoteName = relation.getRemoteName();
    val remoteName = maybeRemoteName != null ? maybeRemoteName : "";
    return Match(relation.getSyncToRemoteStatus()).of(
        Case($(isIn(NoRemotes, InSyncToRemote)), ""),
        Case($(Untracked),
            getString("string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-remote-status-text.untracked")),
        Case($(AheadOfRemote),
            getString("string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-remote-status-text.ahead-of-remote")
                .format(remoteName)),
        Case($(BehindRemote),
            getString("string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-remote-status-text.behind-remote")
                .format(remoteName)),
        Case($(DivergedFromAndNewerThanRemote),
            getString(
                "string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-remote-status-text.diverged-from-and-newer-than-remote")
                    .format(remoteName)),
        Case($(DivergedFromAndOlderThanRemote),
            getString(
                "string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-remote-status-text.diverged-from-and-older-than-remote")
                    .format(remoteName)));
  }

  @UIEffect
  private void setBranchToolTipText(IManagedBranchSnapshot branch) {
    if (branch.isRoot()) {
      setToolTipText(getRootToolTipText(branch.asRoot()));
    } else {
      setToolTipText(getSyncToParentStatusBasedToolTipText(branch.asNonRoot()));
    }
  }

  private static String getSyncToParentStatusBasedToolTipText(INonRootManagedBranchSnapshot branch) {
    val currentBranchName = escapeHtml4(branch.getName());
    val parentBranchName = escapeHtml4(branch.getParent().getName());
    return Match(branch.getSyncToParentStatus()).of(
        Case($(InSync),
            getString("string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-parent-status-tooltip.in-sync.HTML")
                .format(currentBranchName, parentBranchName)),
        Case($(InSyncButForkPointOff),
            getString(
                "string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-parent-status-tooltip.in-sync-but-fork-point-off.HTML")
                    .format(currentBranchName, parentBranchName)),
        Case($(OutOfSync),
            getString("string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-parent-status-tooltip.out-of-sync.HTML")
                .format(currentBranchName, parentBranchName)),
        Case($(MergedToParent),
            getString(
                "string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-parent-status-tooltip.merged-to-parent.HTML")
                    .format(currentBranchName, parentBranchName)));
  }

  private static String getRootToolTipText(IRootManagedBranchSnapshot branch) {
    return getString("string.GitMachete.BranchOrCommitCellRendererComponent.sync-to-parent-status-tooltip.root.HTML")
        .format(branch.getName());
  }

  private static class MyTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    @UIEffect
    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {
      Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      val backgroundColor = isSelected ? UIUtil.getListSelectionBackground(table.hasFocus()) : UIUtil.getListBackground();
      component.setBackground(backgroundColor);
      return component;
    }
  }

  @UIEffect
  private void applyHighlighters(
      Component rendererComponent,
      int row,
      int column,
      boolean hasFocus,
      final boolean selected) {
    CellStyle style = getStyle(row, column, hasFocus, selected);
    rendererComponent.setBackground(style.getBackground());
    rendererComponent.setForeground(style.getForeground());
  }

  @UIEffect
  private CellStyle getStyle(int row, int column, boolean hasFocus, boolean selected) {
    Component dummyRendererComponent = myTableCellRenderer.getTableCellRendererComponent(
        graphTable, /* value */ "", selected, hasFocus, row, column);
    val background = dummyRendererComponent.getBackground();
    val foreground = dummyRendererComponent.getForeground();
    // Theoretically the result of getBackground/getForeground can be null.
    // In our case, we have two factors that guarantee us non-null result.
    // - DefaultTableCellRenderer::getTableCellRendererComponent sets the non-null values for us
    // - both getters look for the non-null values from the parent component (for a cell this is the table)
    assert background != null && foreground != null : "foreground or background color is null";
    return new CellStyle(background, foreground);
  }

  @Data
  private static final class CellStyle {
    private final Color background;
    private final Color foreground;
  }
}
