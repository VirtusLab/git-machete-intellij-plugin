package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.virtuslab.gitmachete.frontend.defs.ActionIds.CHECK_OUT;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.defs.ActionGroupIds;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;

class EnhancedGraphTableMouseAdapter extends MouseAdapter {
  private final EnhancedGraphTable graphTable;
  private final EnhancedGraphTablePopupMenuListener popupMenuListener;

  @UIEffect
  EnhancedGraphTableMouseAdapter(EnhancedGraphTable graphTable) {
    this.graphTable = graphTable;
    this.popupMenuListener = new EnhancedGraphTablePopupMenuListener(graphTable);
  }

  @Override
  @UIEffect
  public void mouseClicked(MouseEvent e) {
    Point point = e.getPoint();
    int row = graphTable.rowAtPoint(point);
    int col = graphTable.columnAtPoint(point);

    // check if we click on one of the branches
    if (row < 0 || col < 0) {
      return;
    }

    BranchOrCommitCell cell = (BranchOrCommitCell) graphTable.getModel().getValueAt(row, col);
    IGraphItem graphItem = cell.getGraphItem();
    if (!graphItem.isBranchItem()) {
      return;
    }

    graphTable.setSelectedBranchName(graphItem.asBranchItem().getBranch().getName());
    performActionAfterChecks(e, point);
  }

  @UIEffect
  private void performActionAfterChecks(MouseEvent e, Point point) {
    ActionManager actionManager = ActionManager.getInstance();
    if (SwingUtilities.isRightMouseButton(e) || isCtrlClick(e)) {
      ActionGroup contextMenuActionGroup = (ActionGroup) actionManager.getAction(ActionGroupIds.CONTEXT_MENU);
      val actionPopupMenu = actionManager.createActionPopupMenu(ActionPlaces.CONTEXT_MENU, contextMenuActionGroup);
      JPopupMenu popupMenu = actionPopupMenu.getComponent();
      popupMenu.addPopupMenuListener(popupMenuListener);
      popupMenu.show(graphTable, (int) point.getX(), (int) point.getY());
    } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {

      val gitMacheteRepositorySnapshot = graphTable.getGitMacheteRepositorySnapshot();
      if (gitMacheteRepositorySnapshot != null) {
        val currentBranchIfManaged = gitMacheteRepositorySnapshot
            .getCurrentBranchIfManaged();
        val isSelectedEqualToCurrent = currentBranchIfManaged != null
            && currentBranchIfManaged.getName().equals(graphTable.getSelectedBranchName());
        if (isSelectedEqualToCurrent) {
          return;
        }
      }

      e.consume();
      DataContext dataContext = DataManager.getInstance().getDataContext(graphTable);
      val actionEvent = AnActionEvent.createFromDataContext(ActionPlaces.CONTEXT_MENU, new Presentation(), dataContext);
      actionManager.getAction(CHECK_OUT).actionPerformed(actionEvent);
    }
  }

  // this method is needed as some macOS users use Ctrl + left-click as a replacement for the right-click
  @UIEffect
  private boolean isCtrlClick(MouseEvent e) {
    return e.isControlDown() && SwingUtilities.isLeftMouseButton(e);
  }
}
