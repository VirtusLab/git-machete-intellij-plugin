package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.actionids.ActionIds.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.frontend.actionids.ActionIds.ACTION_OPEN_MACHETE_FILE;
import static com.virtuslab.gitmachete.frontend.actionids.ActionPlaces.ACTION_PLACE_CONTEXT_MENU;
import static com.virtuslab.gitmachete.frontend.actionids.ActionPlaces.ACTION_PLACE_EMPTY_TEXT;
import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static com.virtuslab.gitmachete.frontend.ui.impl.table.GitPathUtils.getMacheteFilePath;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
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
import com.intellij.ui.GuiUtils;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.JBUI;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actionids.ActionGroupIds;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.graph.api.coloring.GraphItemColorToJBColorMapper;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainterFactory;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphFactory;
import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCellRenderer;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

// TODO (#99): consider applying SpeedSearch for branches and commits
public final class GitMacheteGraphTable extends BaseGraphTable implements DataProvider {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendUiTable");

  private final Project project;
  private final IGitRepositorySelectionProvider gitRepositorySelectionProvider;

  private final IBranchLayoutReader branchLayoutReader;
  private final IRepositoryGraphFactory repositoryGraphFactory;

  @Getter
  @Setter
  @UIEffect
  private boolean isListingCommits;

  @Nullable
  private IGitMacheteRepository gitMacheteRepository;

  @Nullable
  private String selectedBranchName;

  @UIEffect
  public GitMacheteGraphTable(Project project, IGitRepositorySelectionProvider gitRepositorySelectionProvider) {
    super(new GraphTableModel(IRepositoryGraphFactory.NULL_REPOSITORY_GRAPH));

    this.project = project;
    this.gitRepositorySelectionProvider = gitRepositorySelectionProvider;
    this.branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
    this.repositoryGraphFactory = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphFactory.class);
    this.isListingCommits = false;

    // InitializationChecker allows us to invoke the below methods because the class is final
    // and all `@NonNull` fields are already initialized. `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTable.class)`, as would be with a non-final class) at this point.

    var graphCellPainterFactory = RuntimeBinding.instantiateSoleImplementingClass(IGraphCellPainterFactory.class);
    var graphCellPainter = graphCellPainterFactory.create(/* colorProvider */ GraphItemColorToJBColorMapper::getColor,
        /* table */ this);

    initColumns();

    @UI BranchOrCommitCellRenderer branchOrCommitCellRenderer = new BranchOrCommitCellRenderer(this, graphCellPainter);
    setDefaultRenderer(BranchOrCommitCell.class, branchOrCommitCellRenderer);

    setCellSelectionEnabled(false);
    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);

    addMouseListener(new GitMacheteGraphTableMouseAdapter());

    subscribeToVcsRootChanges();
    subscribeToGitRepositoryChanges();
  }

  private void subscribeToVcsRootChanges() {
    // The method reference is invoked when user changes repository in combo box menu
    gitRepositorySelectionProvider.addSelectionChangeObserver(() -> queueRepositoryUpdateAndModelRefresh());
  }

  private void subscribeToGitRepositoryChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    GitRepositoryChangeListener listener = repository -> queueRepositoryUpdateAndModelRefresh();
    project.getMessageBus().connect().subscribe(topic, listener);
  }

  @UIEffect
  public void refreshModel(Path macheteFilePath, boolean isMacheteFilePresent) {
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
            "Provided machete file (${macheteFilePath}) is empty.",
            "Open machete file", getOpenMacheteFileActionAsRunnable());
        LOG.info("Machete file (${macheteFilePath}) is empty");
      }
    }

    setModel(new GraphTableModel(repositoryGraph));

    if (!isMacheteFilePresent) {
      setTextForEmptyGraph(
          "There is no machete file (${macheteFilePath}) for this repository.",
          "Create & open machete file", getOpenMacheteFileActionAsRunnable());
      LOG.info("Machete file (${macheteFilePath}) is absent");
    }

    repaint();
    revalidate();
  }

  @UIEffect
  private Runnable getOpenMacheteFileActionAsRunnable() {
    var action = ActionManager.getInstance().getAction(ACTION_OPEN_MACHETE_FILE);
    var dataContext = DataManager.getInstance().getDataContext(GitMacheteGraphTable.this);
    var anActionEvent = AnActionEvent.createFromDataContext(ACTION_PLACE_EMPTY_TEXT, new Presentation(), dataContext);
    return () -> GuiUtils.invokeLaterIfNeeded(() -> action.actionPerformed(anActionEvent), NON_MODAL);
  }

  @Override
  @UIEffect
  public void refreshModel(@Nullable IGitMacheteRepository newGitMacheteRepository, Path macheteFilePath,
      boolean isMacheteFilePresent) {
    this.gitMacheteRepository = newGitMacheteRepository;
    refreshModel(macheteFilePath, isMacheteFilePresent);
  }

  @Override
  @UIEffect
  public void refreshModel() {
    Option<GitRepository> gitRepository = gitRepositorySelectionProvider.getSelectedRepository();
    if (gitRepository.isDefined()) {
      // A bit of a shortcut: we're accessing filesystem even though we're on UI thread here;
      // this shouldn't ever be a heavyweight operation, however.
      Path macheteFilePath = getMacheteFilePath(gitRepository.get());
      boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

      refreshModel(macheteFilePath, isMacheteFilePresent);
    } else {
      LOG.warn("No git repository selected, not updating the model");
    }
  }

  @UIEffect
  private void setTextForEmptyGraph(String upperText, String lowerText, Runnable action) {
    var attrs = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.Link.linkColor());
    getEmptyText().setText(upperText).appendSecondaryText(lowerText, attrs, /* listener */ e -> action.run());
  }

  @UIEffect
  private void initColumns() {
    createDefaultColumnsFromModel();

    // Otherwise sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);
  }

  @Override
  public void queueRepositoryUpdateAndModelRefresh() {
    LOG.debug("Entering");

    if (project != null && !project.isDisposed()) {
      GuiUtils.invokeLaterIfNeeded(() -> {
        Option<GitRepository> gitRepository = gitRepositorySelectionProvider.getSelectedRepository();
        if (gitRepository.isDefined()) {
          LOG.debug("Queuing repository update onto a non-UI thread");

          GitMacheteRepositoryUpdateTask.of(/* graphTable */ this, project, gitRepository.get(), branchLayoutReader).queue();
        } else {
          LOG.warn("Selected repository is null");
        }
      }, NON_MODAL);
    } else {
      LOG.debug("project == null or is disposed");
    }
  }

  @Override
  @Nullable
  public Object getData(String dataId) {
    return Match(dataId).of(
        // Other keys are handled up the container hierarchy, in GitMachetePanel.
        typeSafeCase(DataKeys.KEY_GRAPH_TABLE, this),
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
