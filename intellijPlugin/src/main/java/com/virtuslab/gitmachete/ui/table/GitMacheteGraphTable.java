package com.virtuslab.gitmachete.ui.table;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.vcs.log.paint.GraphCellPainter;
import com.intellij.vcs.log.paint.SimpleGraphCellPainter;
import com.virtuslab.gitmachete.actions.CheckoutBranchAction;
import com.virtuslab.gitmachete.graph.SyncToParentStatusEdgeColorGenerator;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCellRenderer;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;

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

    addMouseListener(new GitMacheteGraphTableMouseAdapter(this));
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

  protected class GitMacheteGraphTableMouseAdapter extends MouseAdapter {
    // This group with id "GitMachete.ContextMenu" is defined in plugin.xml file
    private static final String GROUP_TO_INVOKE_AS_CONTEXT_MENU = "GitMachete.ContextMenu";

    private final GitMacheteGraphTable graphTable;

    public GitMacheteGraphTableMouseAdapter(GitMacheteGraphTable graphTable) {
      this.graphTable = graphTable;
    }

    public void mouseClicked(MouseEvent e) {
      if (SwingUtilities.isRightMouseButton(e)) {
        Point point = e.getPoint();
        int row = rowAtPoint(point);
        int col = columnAtPoint(point);

        // check if we click on one of branches
        if (row < 0 || col < 0) return;

        String branchName = getValueAt(rowAtPoint(point), columnAtPoint(point)).toString();
        CheckoutBranchAction.setNameOfBranchToCheckout(branchName);

        ActionPopupMenu actionPopupMenu =
            ActionManager.getInstance()
                .createActionPopupMenu(
                    ActionPlaces.UNKNOWN,
                    (ActionGroup)
                        ActionManager.getInstance().getAction(GROUP_TO_INVOKE_AS_CONTEXT_MENU));
        actionPopupMenu.getComponent().show(graphTable, (int) point.getX(), (int) point.getY());
      }
    }
  }
}
