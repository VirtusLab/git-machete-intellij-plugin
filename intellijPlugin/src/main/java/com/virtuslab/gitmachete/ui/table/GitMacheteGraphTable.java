package com.virtuslab.gitmachete.ui.table;

import static com.virtuslab.gitmachete.ui.table.GraphTableModel.BRANCH_OR_COMMIT_COLUMN;

import com.intellij.openapi.util.Couple;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.vcs.log.graph.DefaultColorGenerator;
import com.intellij.vcs.log.paint.GraphCellPainter;
import com.intellij.vcs.log.paint.SimpleGraphCellPainter;
import com.virtuslab.gitmachete.graph.repositoryGraph.IRepositoryGraph;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCellRenderer;
import javax.annotation.Nonnull;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

/* todo: consider applying SpeedSearch for branches and commits */
public class GitMacheteGraphTable extends JBTable {

  private static final String GIT_MACHETE_TEXT = "Git Machete Status";

  public GitMacheteGraphTable(@Nonnull IRepositoryGraph IRepositoryGraph) {
    super(new GraphTableModel(IRepositoryGraph));

    GraphCellPainter graphCellPainter =
        new SimpleGraphCellPainter(new DefaultColorGenerator()) {
          @Override
          protected int getRowHeight() {
            return GitMacheteGraphTable.this.getRowHeight();
          }
        };
    BranchOrCommitCellRenderer branchOrCommitCellRenderer =
        new BranchOrCommitCellRenderer(GitMacheteGraphTable.this, graphCellPainter);

    getEmptyText().setText(GIT_MACHETE_TEXT);

    initColumns();

    setDefaultRenderer(BranchOrCommitCell.class, branchOrCommitCellRenderer);

    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(this, false);
  }

  private void initColumns() {
    createDefaultColumnsFromModel();
    setAutoCreateColumnsFromModel(
        false); // otherwise sizes are recalculated after each TableColumn re-initialization
  }

  @Override
  @Nonnull
  public GraphTableModel getModel() {
    return (GraphTableModel) super.getModel();
  }

  private void viewportSet(JViewport viewport) {
    viewport.addChangeListener(
        e -> {
          AbstractTableModel model = getModel();
          Couple<Integer> visibleRows = ScrollingUtil.getVisibleRows(this);
          model.fireTableChanged(
              new TableModelEvent(
                  model, visibleRows.first - 1, visibleRows.second, BRANCH_OR_COMMIT_COLUMN));
        });
  }

  @Nonnull
  public static JScrollPane setupScrolledGraph(
      @Nonnull GitMacheteGraphTable graphTable, int border) {
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(graphTable, border);
    graphTable.viewportSet(scrollPane.getViewport());
    return scrollPane;
  }
}
