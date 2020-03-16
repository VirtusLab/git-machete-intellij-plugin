package com.virtuslab.gitmachete.frontend.ui.table;

import static com.virtuslab.gitmachete.frontend.actions.ActionIDs.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.frontend.actions.ActionIDs.GROUP_TO_INVOKE_AS_CONTEXT_MENU;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

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

import com.virtuslab.gitmachete.frontend.actions.DataKeyIDs;
import com.virtuslab.gitmachete.frontend.graph.GraphEdgeColorToJBColorMapper;
import com.virtuslab.gitmachete.frontend.graph.model.IGraphElement;
import com.virtuslab.gitmachete.frontend.ui.GitMacheteGraphTableManager;
import com.virtuslab.gitmachete.frontend.ui.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.cell.BranchOrCommitCellRenderer;

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

    GraphCellPainter graphCellPainter = new SimpleGraphCellPainter(GraphEdgeColorToJBColorMapper::getColor) {
      @Override
      protected int getRowHeight() {
        return GitMacheteGraphTable.this.getRowHeight();
      }
    };
    BranchOrCommitCellRenderer branchOrCommitCellRenderer = new BranchOrCommitCellRenderer(GitMacheteGraphTable.this,
        graphCellPainter);

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
  public Object getData(@Nonnull String dataId) {
    return Match(dataId).of(
        Case($(CommonDataKeys.PROJECT.getName()), project),
        // We must use `getSelectedTextEditor()` instead of `getSelectedEditor()` because we must return class
        // com.intellij.openapi.editor.Editor
        Case($(CommonDataKeys.EDITOR.getName()), FileEditorManager.getInstance(project).getSelectedTextEditor()),
        Case($(DataKeyIDs.KEY_TABLE_MANAGER_STRING), tableManager),
        Case($(DataKeyIDs.KEY_SELECTED_BRANCH_NAME_STRING), selectedBranchName),
        Case($(CommonDataKeys.PROJECT.getName()), project),
        Case($(), () -> null));
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
        ActionGroup contextMenuGroup = (ActionGroup) ActionManager.getInstance()
            .getAction(GROUP_TO_INVOKE_AS_CONTEXT_MENU);
        ActionPopupMenu actionPopupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN,
            contextMenuGroup);
        actionPopupMenu.getComponent().show(graphTable, (int) point.getX(), (int) point.getY());
      } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {
        e.consume();
        AnActionEvent actionEvent = new AnActionEvent(/* inputEvent */ null,
            DataManager.getInstance().getDataContext(graphTable), ActionPlaces.UNKNOWN, new Presentation(),
            ActionManager.getInstance(), /* modifiers */ 0);
        ActionManager.getInstance().getAction(ACTION_CHECK_OUT).actionPerformed(actionEvent);
      }
    }
  }
}
