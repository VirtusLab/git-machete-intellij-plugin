package com.virtuslab.gitmachete.frontend.ui.impl.cell;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.ui.render.LabelPainter;
import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainterFactory;
import com.virtuslab.gitmachete.frontend.graph.api.paint.PaintParameters;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGitMacheteRepositorySnapshotProvider;
import io.vavr.collection.List;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.NoRemotes;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;
import static com.virtuslab.gitmachete.frontend.defs.Colors.ORANGE;
import static com.virtuslab.gitmachete.frontend.defs.Colors.RED;
import static com.virtuslab.gitmachete.frontend.defs.Colors.TRANSPARENT;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.isIn;

@SuppressWarnings("allcheckers")
public class BranchOrCommitCellRenderer implements TableCellRenderer {
    private static final IGraphCellPainterFactory graphCellPainterFactoryInstance = RuntimeBinding.instantiateSoleImplementingClass(IGraphCellPainterFactory.class);
    private BufferedImage graphImage = null;

    @Override
    @UIEffect
    public Component getTableCellRendererComponent(JTable table, Object object, boolean isSelected, boolean hasFocus, int row, int column) {
        if (graphImage == null) {
            assert table instanceof IGitMacheteRepositorySnapshotProvider : "Table variable is not instance of ${IGitMacheteRepositorySnapshotProvider.class.getSimpleName()}";
            var gitMacheteRepositorySnapshot = ((IGitMacheteRepositorySnapshotProvider) table).getGitMacheteRepositorySnapshot();

            assert object instanceof BranchOrCommitCell : "value is not an instance of " + BranchOrCommitCell.class.getSimpleName();
            var cell = (BranchOrCommitCell) object;

            IGraphItem graphItem = cell.getGraphItem();
            int maxGraphNodePositionInRow = getMaxGraphNodePositionInRow(graphItem);
            List<? extends IRenderPart> renderParts;
            if (graphItem.hasBulletPoint()) {
                renderParts = cell.getRenderParts();
            } else {
                renderParts = cell.getRenderParts().filter(e -> !e.isNode());
            }
            graphImage = getGraphImage(table, maxGraphNodePositionInRow);
            Graphics2D g2 = graphImage.createGraphics();
            var graphCellPainter = graphCellPainterFactoryInstance.create(table);
            graphCellPainter.draw(g2, renderParts);
        }

        JPanel panel = new JPanel();
        JPanel panel2 = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel2.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JBLabel("Ala ma kota");
        panel.setToolTipText("Test");
        panel.add(label);
        panel2.add(panel);

        var backgroundColor = isSelected ? UIUtil.getListSelectionBackground(table.hasFocus()) : UIUtil.getListBackground();
        panel.setBackground(backgroundColor);

        panel2.updateUI();
        panel2.revalidate();

        int height = panel2.getHeight();

        return panel2;
    }

    private static @NonNegative int getMaxGraphNodePositionInRow(IGraphItem graphItem) {
        // If item is a child (non root) branch, then the text must be shifted right to make place
        // for the corresponding the right edge to the left.
        // If item is a commit, then the text must be shifted right to keep it horizontally aligned
        // with the corresponding branch item.
        boolean isRootBranch = graphItem.isBranchItem() && graphItem.asBranchItem().getBranch().isRootBranch();
        return graphItem.getIndentLevel() + (isRootBranch ? 0 : 1);
    }

    @UIEffect
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

    private static JBColor getColor(SyncToRemoteStatus relation) {
        return Match(relation.getRelation()).of(
                Case($(isIn(NoRemotes, InSyncToRemote)), TRANSPARENT),
                Case($(Untracked), ORANGE),
                Case($(isIn(AheadOfRemote, BehindRemote, DivergedFromAndNewerThanRemote, DivergedFromAndOlderThanRemote)), RED));
    }

    private static String getLabel(SyncToRemoteStatus status) {
        var remoteName = status.getRemoteName();
        return Match(status.getRelation()).of(
                Case($(isIn(NoRemotes, InSyncToRemote)), ""),
                Case($(Untracked), "  (untracked)"),
                Case($(AheadOfRemote), "  (ahead of ${remoteName})"),
                Case($(BehindRemote), "  (behind ${remoteName})"),
                // To avoid clutter we omit `& newer than` part in status label, coz this is default situation
                Case($(DivergedFromAndNewerThanRemote), "  (diverged from ${remoteName})"),
                Case($(DivergedFromAndOlderThanRemote), "  (diverged from & older than ${remoteName})"));
    }
}
