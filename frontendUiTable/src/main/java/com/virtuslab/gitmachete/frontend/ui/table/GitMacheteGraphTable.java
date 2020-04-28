package com.virtuslab.gitmachete.frontend.ui.table;

import static com.virtuslab.gitmachete.frontend.actionids.ActionIds.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.frontend.actionids.ActionIds.GROUP_TO_INVOKE_AS_CONTEXT_MENU;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StatusText;
import git4idea.repo.GitRepository;
import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.graph.api.coloring.GraphItemColorToJBColorMapper;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainterFactory;
import com.virtuslab.gitmachete.frontend.ui.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.cell.BranchOrCommitCellRenderer;
import com.virtuslab.gitmachete.frontend.ui.selection.ISelectionChangeObservable;

// TODO (#99): consider applying SpeedSearch for branches and commits
public final class GitMacheteGraphTable extends JBTable implements DataProvider {
  private final GraphTableModel graphTableModel;
  private final Project project;
  private final AtomicReference<@Nullable IGitMacheteRepository> gitMacheteRepositoryRef;
  private final ISelectionChangeObservable<GitRepository> selectionChangeObservable;

  @Nullable
  private IBranchLayout branchLayout;

  @Nullable
  private Path macheteFilePath;

  @Nullable
  private String selectedBranchName;

  @UIEffect
  public GitMacheteGraphTable(
      GraphTableModel graphTableModel,
      Project project,
      AtomicReference<@Nullable IGitMacheteRepository> gitMacheteRepositoryRef,
      ISelectionChangeObservable<GitRepository> selectionChangeObservable) {
    super(graphTableModel);

    this.graphTableModel = graphTableModel;
    this.project = project;
    this.gitMacheteRepositoryRef = gitMacheteRepositoryRef;
    this.selectionChangeObservable = selectionChangeObservable;

    // InitializationChecker allows us to invoke the below methods because the class is final
    // and all `@NonNull` fields are already initialized. `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTableManager.class)`, as would be with a non-final class) at this point.

    var graphCellPainterFactory = RuntimeBinding.instantiateSoleImplementingClass(IGraphCellPainterFactory.class);
    var graphCellPainter = graphCellPainterFactory.create(/* colorProvider */ GraphItemColorToJBColorMapper::getColor,
        /* table */ this);

    initColumns();

    @SuppressWarnings("guieffect:assignment.type.incompatible")
    @AlwaysSafe
    BranchOrCommitCellRenderer branchOrCommitCellRenderer = new BranchOrCommitCellRenderer(/* table */ this,
        graphCellPainter);
    setDefaultRenderer(BranchOrCommitCell.class, branchOrCommitCellRenderer);

    setCellSelectionEnabled(false);
    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);

    addMouseListener(new GitMacheteGraphTableMouseAdapter(/* graphTable */ this));
  }

  @UIEffect
  public void setTextForEmptyGraph(String upperText, String lowerText) {
    getEmptyText().setText(upperText).appendSecondaryText(lowerText, StatusText.DEFAULT_ATTRIBUTES,
        /* listener */ null);
  }

  @UIEffect
  private void initColumns() {
    createDefaultColumnsFromModel();

    // Otherwise sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);
  }

  @Override
  public GraphTableModel getModel() {
    return graphTableModel;
  }

  private <T> Match.Case<String, T> typeSafeCase(DataKey<T> key, T value) {
    return Case($(key.getName()), value);
  }

  @Override
  @Nullable
  public Object getData(String dataId) {
    var gitMacheteRepository = gitMacheteRepositoryRef.get();
    return Match(dataId).of(
        typeSafeCase(DataKeys.KEY_BRANCH_LAYOUT, branchLayout),
        typeSafeCase(DataKeys.KEY_GIT_MACHETE_FILE_PATH, macheteFilePath),
        typeSafeCase(DataKeys.KEY_IS_GIT_MACHETE_REPOSITORY_READY, gitMacheteRepository != null),
        typeSafeCase(DataKeys.KEY_GIT_MACHETE_REPOSITORY, gitMacheteRepository),
        typeSafeCase(DataKeys.KEY_SELECTED_BRANCH_NAME, selectedBranchName),
        typeSafeCase(DataKeys.KEY_SELECTED_VCS_REPOSITORY, selectionChangeObservable.getValue()),
        typeSafeCase(CommonDataKeys.PROJECT, project),
        Case($(), (Object) null));
  }

  public void setBranchLayout(IBranchLayout newBranchLayout) {
    this.branchLayout = newBranchLayout;
  }

  public void setMacheteFilePath(Path newMacheteFilePath) {
    this.macheteFilePath = newMacheteFilePath;
  }

  protected class GitMacheteGraphTableMouseAdapter extends MouseAdapter {

    private final GitMacheteGraphTable graphTable;

    @UIEffect
    public GitMacheteGraphTableMouseAdapter(GitMacheteGraphTable graphTable) {
      this.graphTable = graphTable;
    }

    @UIEffect
    public void mouseClicked(MouseEvent e) {
      Point point = e.getPoint();
      int row = rowAtPoint(point);
      int col = columnAtPoint(point);

      // check if we click on one of branches
      if (row < 0 || col < 0) {
        return;
      }

      BranchOrCommitCell cell = (BranchOrCommitCell) getModel().getValueAt(row, col);
      IGraphItem graphItem = cell.getGraphItem();
      if (!graphItem.isBranchItem()) {
        return;
      }

      selectedBranchName = graphItem.getValue();

      if (SwingUtilities.isRightMouseButton(e)) {
        ActionGroup contextMenuGroup = (ActionGroup) ActionManager.getInstance()
            .getAction(GROUP_TO_INVOKE_AS_CONTEXT_MENU);
        ActionPopupMenu actionPopupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN,
            contextMenuGroup);
        actionPopupMenu.getComponent().show(graphTable, (int) point.getX(), (int) point.getY());
      } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {
        e.consume();
        AnActionEvent actionEvent = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, new Presentation(),
            DataManager.getInstance().getDataContext(graphTable));
        ActionManager.getInstance().getAction(ACTION_CHECK_OUT).actionPerformed(actionEvent);
      }
    }
  }
}
