package com.virtuslab.gitmachete.ui.table;

import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.vcs.log.graph.DefaultColorGenerator;
import com.intellij.vcs.log.paint.GraphCellPainter;
import com.intellij.vcs.log.paint.SimpleGraphCellPainter;
import com.virtuslab.gitmachete.api.IRepositoryGraph;
import com.virtuslab.gitmachete.ui.render.GraphBranchCell;
import com.virtuslab.gitmachete.ui.render.GraphBranchCellRenderer;
import javax.annotation.Nonnull;

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
    GraphBranchCellRenderer myGraphBranchCellRenderer =
        new GraphBranchCellRenderer(graphCellPainter, GitMacheteGraphTable.this);

    getEmptyText().setText(GIT_MACHETE_TEXT);

    initColumns();

    setDefaultRenderer(GraphBranchCell.class, myGraphBranchCellRenderer);

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
}
