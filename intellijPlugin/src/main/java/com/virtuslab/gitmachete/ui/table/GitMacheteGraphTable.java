package com.virtuslab.gitmachete.ui.table;

import static com.virtuslab.gitmachete.actions.ActionIDs.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.actions.ActionIDs.GROUP_TO_INVOKE_AS_CONTEXT_MENU;
import static com.virtuslab.gitmachete.actions.DataKeyIDs.KEY_SELECTED_BRANCH_NAME_STRING;
import static com.virtuslab.gitmachete.actions.DataKeyIDs.KEY_TABLE_MANAGER_STRING;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.vcs.log.paint.GraphCellPainter;
import com.intellij.vcs.log.paint.SimpleGraphCellPainter;
import com.virtuslab.gitmachete.graph.GraphEdgeColorEdgeJBColorGenerator;
import com.virtuslab.gitmachete.graph.model.IGraphElement;
import com.virtuslab.gitmachete.ui.GitMacheteGraphTableManager;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.ui.cell.BranchOrCommitCellRenderer;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO (#99): consider applying SpeedSearch for branches and commits
public class GitMacheteGraphTable extends JBTable implements DataProvider {
  private static final String GIT_MACHETE_TEXT = "Git Machete Status";

  private final Project project;
  private final GitMacheteGraphTableManager tableManager;

  private String selectedBranchName;

  public GitMacheteGraphTable(
      @Nonnull GraphTableModel graphTableModel,
      Project project,
      GitMacheteGraphTableManager tableManager) {
    super(graphTableModel);

    this.project = project;
    this.tableManager = tableManager;

    GraphCellPainter graphCellPainter =
        new SimpleGraphCellPainter(new GraphEdgeColorEdgeJBColorGenerator()) {
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

  @Nullable
  @Override
  public Object getData(@NotNull String dataId) {
    // TODO (#40): replace with Vavr (Case)
    if (dataId.equals(CommonDataKeys.PROJECT.getName())) {
      return project;
    } else if (dataId.equals(CommonDataKeys.EDITOR.getName())) {
      return FileEditorManager.getInstance(project).getSelectedEditor();
    } else if (dataId.equals(KEY_TABLE_MANAGER_STRING)) {
      return tableManager;
    } else if (dataId.equals(KEY_SELECTED_BRANCH_NAME_STRING)) {
      return selectedBranchName;
    }
    return null;
  }

  protected class GitMacheteGraphTableMouseAdapter extends MouseAdapter {

    private final GitMacheteGraphTable graphTable;

    public GitMacheteGraphTableMouseAdapter(GitMacheteGraphTable graphTable) {
      this.graphTable = graphTable;
    }

    public void mouseClicked(MouseEvent e) {
      Point point = e.getPoint();
      int row = rowAtPoint(point);
      int col = columnAtPoint(point);

      // check if we click on one of branches
      if (row < 0 || col < 0) {
        return;
      }

      BranchOrCommitCell cell = (BranchOrCommitCell) getModel().getValueAt(row, col);
      IGraphElement element = cell.getElement();
      if (!element.isBranch()) {
        return;
      }

      selectedBranchName = element.getValue();

      if (SwingUtilities.isRightMouseButton(e)) {
        ActionGroup contextMenuGroup =
            (ActionGroup) ActionManager.getInstance().getAction(GROUP_TO_INVOKE_AS_CONTEXT_MENU);
        ActionPopupMenu actionPopupMenu =
            ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.UNKNOWN, contextMenuGroup);
        actionPopupMenu.getComponent().show(graphTable, (int) point.getX(), (int) point.getY());
      } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {
        e.consume();
        AnActionEvent actionEvent =
            new AnActionEvent(
                /*inputEvent*/ null,
                DataManager.getInstance().getDataContext(graphTable),
                ActionPlaces.UNKNOWN,
                new Presentation(),
                ActionManager.getInstance(),
                /*modifiers*/ 0);
        ActionManager.getInstance().getAction(ACTION_CHECK_OUT).actionPerformed(actionEvent);
      }
    }
  }
}
