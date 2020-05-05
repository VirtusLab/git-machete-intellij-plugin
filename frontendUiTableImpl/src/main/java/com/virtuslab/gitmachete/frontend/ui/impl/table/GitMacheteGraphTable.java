package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.virtuslab.gitmachete.frontend.actionids.ActionIds.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.frontend.actionids.ActionPlaces.ACTION_PLACE_CONTEXT_MENU;
import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StatusText;
import lombok.Setter;
import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actionids.ActionGroupIds;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.graph.api.coloring.GraphItemColorToJBColorMapper;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainterFactory;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphFactory;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCellRenderer;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

// TODO (#99): consider applying SpeedSearch for branches and commits
public final class GitMacheteGraphTable extends JBTable implements DataProvider {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendUiTable");

  private final Project project;
  private final IRepositoryGraphFactory repositoryGraphFactory;

  @Setter
  @Nullable
  private IBranchLayout branchLayout;

  @Setter
  @Nullable
  private IBranchLayoutWriter branchLayoutWriter;

  @Nullable
  private IGitMacheteRepository gitMacheteRepository;

  @Nullable
  private String selectedBranchName;

  @UIEffect
  public GitMacheteGraphTable(Project project) {
    super(new GraphTableModel(IRepositoryGraphFactory.NULL_REPOSITORY_GRAPH));

    this.project = project;
    this.repositoryGraphFactory = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphFactory.class);

    // InitializationChecker allows us to invoke the below methods because the class is final
    // and all `@NonNull` fields are already initialized. `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTable.class)`, as would be with a non-final class) at this point.

    var graphCellPainterFactory = RuntimeBinding.instantiateSoleImplementingClass(IGraphCellPainterFactory.class);
    var graphCellPainter = graphCellPainterFactory.create(/* colorProvider */ GraphItemColorToJBColorMapper::getColor,
        /* table */ this);

    initColumns();

    @SuppressWarnings("guieffect:assignment.type.incompatible")
    @AlwaysSafe
    BranchOrCommitCellRenderer branchOrCommitCellRenderer = new BranchOrCommitCellRenderer(/* table */ this, graphCellPainter);
    setDefaultRenderer(BranchOrCommitCell.class, branchOrCommitCellRenderer);

    setCellSelectionEnabled(false);
    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);

    addMouseListener(new GitMacheteGraphTableMouseAdapter());
  }

  @UIEffect
  public void refreshModel(Path macheteFilePath, boolean isMacheteFilePresent, boolean isListingCommits) {
    LOG.debug(() -> "Entering: macheteFilePath = ${macheteFilePath}, isMacheteFilePresent = ${isMacheteFilePresent}, " +
        "isListingCommits = ${isListingCommits}");

    // TODO (#176): When machete file is absent or empty,
    // propose using branch layout automatically detected by discover functionality

    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.debug("Project is not initialized or application is in unit test mode. Returning.");
      return;
    }

    IRepositoryGraph repositoryGraph;
    if (gitMacheteRepository == null) {
      repositoryGraph = IRepositoryGraphFactory.NULL_REPOSITORY_GRAPH;
    } else {
      repositoryGraph = repositoryGraphFactory.getRepositoryGraph(gitMacheteRepository, isListingCommits);
      if (gitMacheteRepository.getRootBranches().isEmpty()) {
        setTextForEmptyGraph(
            "Your machete file (${macheteFilePath}) is empty.",
            "Please use 'git machete discover' CLI command to automatically fill in the machete file.");
        LOG.info("Machete file (${macheteFilePath}) is empty");
      }
    }

    setModel(new GraphTableModel(repositoryGraph));

    if (!isMacheteFilePresent) {
      setTextForEmptyGraph(
          "There is no machete file (${macheteFilePath}) for this repository.",
          "Please use 'git machete discover' CLI command to automatically create machete file.");
      LOG.info("Machete file (${macheteFilePath}) is absent");
    }

    repaint();
    revalidate();
  }

  @UIEffect
  public void refreshModel(@Nullable IGitMacheteRepository newGitMacheteRepository, Path macheteFilePath,
      boolean isMacheteFilePresent, boolean isListingCommits) {

    this.gitMacheteRepository = newGitMacheteRepository;
    refreshModel(macheteFilePath, isMacheteFilePresent, isListingCommits);
  }

  @UIEffect
  private void setTextForEmptyGraph(String upperText, String lowerText) {
    getEmptyText().setText(upperText).appendSecondaryText(lowerText, StatusText.DEFAULT_ATTRIBUTES, /* listener */ null);
  }

  @UIEffect
  private void initColumns() {
    createDefaultColumnsFromModel();

    // Otherwise sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);
  }

  @Override
  @Nullable
  public Object getData(String dataId) {
    return Match(dataId).of(
        // Other keys are handled up the container hierarchy, in GitMachetePanel.
        typeSafeCase(DataKeys.KEY_BRANCH_LAYOUT, branchLayout),
        typeSafeCase(DataKeys.KEY_BRANCH_LAYOUT_WRITER, branchLayoutWriter),
        typeSafeCase(DataKeys.KEY_GIT_MACHETE_REPOSITORY, gitMacheteRepository),
        typeSafeCase(DataKeys.KEY_SELECTED_BRANCH_NAME, selectedBranchName),
        typeSafeCase(CommonDataKeys.PROJECT, project),
        Case($(), (Object) null));
  }

  private class GitMacheteGraphTableMouseAdapter extends MouseAdapter {
    @UIEffect
    GitMacheteGraphTableMouseAdapter() {}

    @Override
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

      ActionManager actionManager = ActionManager.getInstance();
      if (SwingUtilities.isRightMouseButton(e)) {
        ActionGroup contextMenuActionGroup = (ActionGroup) actionManager.getAction(ActionGroupIds.ACTION_GROUP_CONTEXT_MENU);
        var actionPopupMenu = actionManager.createActionPopupMenu(ACTION_PLACE_CONTEXT_MENU, contextMenuActionGroup);
        actionPopupMenu.getComponent().show(GitMacheteGraphTable.this, (int) point.getX(), (int) point.getY());
      } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {
        e.consume();
        DataContext dataContext = DataManager.getInstance().getDataContext(GitMacheteGraphTable.this);
        var actionEvent = AnActionEvent.createFromDataContext(ACTION_PLACE_CONTEXT_MENU, new Presentation(), dataContext);
        actionManager.getAction(ACTION_CHECK_OUT).actionPerformed(actionEvent);
      }
    }
  }
}
