package com.virtuslab.gitmachete.ui.table;

import com.intellij.ide.DataManager;
import com.intellij.ide.ui.customization.CustomizationUtil;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.vcs.log.paint.GraphCellPainter;
import com.intellij.vcs.log.paint.SimpleGraphCellPainter;
import com.virtuslab.gitmachete.graph.SyncToParentStatusEdgeColorGenerator;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCellRenderer;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* todo: consider applying SpeedSearch for branches and commits */
public class GitMacheteGraphTable extends JBTable {

  private static final String GIT_MACHETE_TEXT = "Git Machete Status";

  public GitMacheteGraphTable(@Nonnull GraphTableModel graphTableModel) {
    super(graphTableModel);

    GraphCellPainter graphCellPainter =
        new SimpleGraphCellPainter(new SyncToParentStatusEdgeColorGenerator()) {
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
    setCellSelectionEnabled(false);

    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(this, false);

    CustomizationUtil.installPopupHandler(this, "GitMachete.ContextMenu", "GitMachete.ContextMenu");

    /*addMouseListener(
    new java.awt.event.MouseAdapter() {

      public void mouseClicked(java.awt.event.MouseEvent e) {

        if (e.getClickCount() == 2 && !e.isConsumed()) {
          e.consume();

          int row = rowAtPoint(e.getPoint());

          int col = columnAtPoint(e.getPoint());



          ActionPopupMenu apm =
                  ActionManager.getInstance()
                          .createActionPopupMenu(
                                  "GitMachete.ContextMenu",
                                  (ActionGroup) ActionManager.getInstance().getAction("GitMachete.ContextMenu"));
          apm.getComponent().show(e.getComponent(), e.getX(), e.getY());

          //System.out.println(
                  //"Value in the cell clicked :" + " " + getValueAt(row, col).toString());
        }
      }
    });*/
  }

  private void initColumns() {
    createDefaultColumnsFromModel();

    // otherwise sizes are recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);
  }

  @Override
  @Nonnull
  public GraphTableModel getModel() {
    return (GraphTableModel) super.getModel();
  }

  /*@Nullable
  @Override
  public Object getData(@NotNull String dataId) {
    if (dataId.equals("project")) {
      DataContext dataContext = DataManager.getInstance().getDataContext();
      Project project = (Project) dataContext.getData(DataConstants.PROJECT);
      return project;
    }
    return dataId;
  }*/
}
