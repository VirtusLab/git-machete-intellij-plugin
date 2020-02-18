package com.virtuslab.gitmachete.ui.table;

import com.intellij.ide.DataManager;
import com.intellij.ide.ui.customization.CustomizationUtil;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
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
  private static final Logger LOG = Logger.getInstance(GitMacheteGraphTable.class);

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

    //CustomizationUtil.installPopupHandler(this, "GitMachete.ContextMenu", "GitMachete.ContextMenu");
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
    private final GitMacheteGraphTable graphTable;

    public GitMacheteGraphTableMouseAdapter(GitMacheteGraphTable graphTable) {
      this.graphTable = graphTable;
    }

    public void mouseClicked(MouseEvent e) {
      System.out.println("CLICK!");
      if (SwingUtilities.isRightMouseButton(e)) {
        Point point = e.getPoint();
        String branchName = getValueAt(rowAtPoint(point), columnAtPoint(point)).toString();
        System.out.println("BRANCH NAME: "+branchName + " r: "+rowAtPoint(point)+" c: "+columnAtPoint(point));
        CheckoutBranchAction.setNameOfBranchToCheckout(branchName);
        ActionPopupMenu actionPopupMenu =
                ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN,(ActionGroup)ActionManager.getInstance().getAction("GitMachete.ContextMenu")) ;
        actionPopupMenu.getComponent().show(graphTable, (int)point.getX(), (int)point.getY());
        /*ActionManager am = ActionManager.getInstance();
        am.getAction("GitMachete.CheckoutBranchAction").actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                ActionPlaces.UNKNOWN, new Presentation(),
                ActionManager.getInstance(), 0));*/
      }
    }
  }
}
