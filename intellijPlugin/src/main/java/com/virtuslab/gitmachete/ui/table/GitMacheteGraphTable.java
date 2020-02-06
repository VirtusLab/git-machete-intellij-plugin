package com.virtuslab.gitmachete.ui.table;

import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.vcs.log.paint.GraphCellPainter;
import com.intellij.vcs.log.paint.SimpleGraphCellPainter;
import com.virtuslab.gitmachete.graph.GitMacheteColorGenerator;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCellRenderer;
import javax.annotation.Nonnull;
import lombok.Getter;

/* todo: consider applying SpeedSearch for branches and commits */
public class GitMacheteGraphTable extends JBTable {
  @Getter private final GraphTableModel graphTableModel;

  private static final String GIT_MACHETE_TEXT = "Git Machete Status";

  public GitMacheteGraphTable(@Nonnull RepositoryGraph repositoryGraph) {
    graphTableModel = new GraphTableModel(repositoryGraph);
    setModel(graphTableModel);

    GraphCellPainter graphCellPainter =
        new SimpleGraphCellPainter(new GitMacheteColorGenerator()) {
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
}
