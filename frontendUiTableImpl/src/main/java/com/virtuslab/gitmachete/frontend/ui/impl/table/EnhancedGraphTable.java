package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.frontend.defs.ActionPlaces.ACTION_PLACE_CONTEXT_MENU;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.JBUI;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.defs.ActionGroupIds;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphCache;
import com.virtuslab.gitmachete.frontend.graph.api.repository.NullRepositoryGraph;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCellRenderer;
import com.virtuslab.gitmachete.frontend.ui.providerservice.BranchLayoutWriterProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

/**
 *  This class compared to {@link SimpleGraphTable} has graph table refreshing and provides
 *  data like last clicked branch name, opened project or {@link IGitMacheteRepositorySnapshot} of current
 *  repository for actions
 */
// TODO (#99): consider applying SpeedSearch for branches and commits
@CustomLog
public final class EnhancedGraphTable extends BaseEnhancedGraphTable
    implements
      DataProvider,
      IGitMacheteRepositorySnapshotProvider {

  private final Project project;

  private final IBranchLayoutReader branchLayoutReader;
  private final IRepositoryGraphCache repositoryGraphCache;

  @Getter
  @Setter
  @UIEffect
  private boolean isListingCommits;

  @Getter
  @UIEffect
  private @Nullable IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  @UIEffect
  private @Nullable String selectedBranchName;

  @UIEffect
  public EnhancedGraphTable(Project project) {
    super(new GraphTableModel(NullRepositoryGraph.getInstance()));

    this.project = project;
    this.branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
    this.repositoryGraphCache = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphCache.class);
    this.isListingCommits = false;

    // InitializationChecker allows us to invoke the below methods because the class is final
    // and all `@NonNull` fields are already initialized. `this` is already `@Initialized` (and not just
    // `@UnderInitialization(EnhancedGraphTable.class)`, as would be with a non-final class) at this point.

    initColumns();

    setCellSelectionEnabled(false);
    setColumnSelectionAllowed(false);
    setRowSelectionAllowed(false);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    setDefaultRenderer(BranchOrCommitCell.class, new BranchOrCommitCellRenderer(/* hasBranchActionToolTips */ true));

    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);

    addMouseListener(new EnhancedGraphTableMouseAdapter( /* outer */ this));

    subscribeToGitRepositoryFilesChanges();
    subscribeToSelectedGitRepositoryChange();
  }

  private IGitRepositorySelectionProvider getGitRepositorySelectionProvider() {
    return project.getService(SelectedGitRepositoryProvider.class).getGitRepositorySelectionProvider();
  }

  private void subscribeToGitRepositoryFilesChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    GitRepositoryChangeListener listener = repository -> queueRepositoryUpdateAndModelRefresh();
    project.getMessageBus().connect().subscribe(topic, listener);
  }

  private void subscribeToSelectedGitRepositoryChange() {
    // The method reference is invoked when user changes repository in selection component menu
    var gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
    gitRepositorySelectionProvider.addSelectionChangeObserver(() -> queueRepositoryUpdateAndModelRefresh());
  }

  @UIEffect
  private void refreshModel(GitRepository gitRepository, List<String> skippedBranchNames) {
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.debug("Project is not initialized or application is in unit test mode. Returning.");
      return;
    }

    // A bit of a shortcut: we're accessing filesystem even though we're on the UI thread here;
    // this shouldn't ever be a heavyweight operation, however.
    Path macheteFilePath = getMacheteFilePath(gitRepository);
    boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

    LOG.debug(() -> "Entering: macheteFilePath = ${macheteFilePath}, isMacheteFilePresent = ${isMacheteFilePresent}, " +
        "isListingCommits = ${isListingCommits}");

    IRepositoryGraph repositoryGraph;
    if (gitMacheteRepositorySnapshot == null) {
      repositoryGraph = NullRepositoryGraph.getInstance();
    } else {
      repositoryGraph = repositoryGraphCache.getRepositoryGraph(gitMacheteRepositorySnapshot, isListingCommits);
      if (gitMacheteRepositorySnapshot.getRootBranches().isEmpty()) {
        LOG.info("Machete file (${macheteFilePath}) is empty, so auto discover is running");
        doAutomaticDiscover(macheteFilePath);
      }
    }

    if (!isMacheteFilePresent) {
      LOG.info("Machete file (${macheteFilePath}) is absent, so auto discover is running");
      doAutomaticDiscover(macheteFilePath);
    }

    setModel(new GraphTableModel(repositoryGraph));

    if (skippedBranchNames.nonEmpty()) {
      // This warning notification will not cover other error notifications (e.g. when rebase errors occur)
      VcsNotifier.getInstance(project).notifyWarning(
          getString("string.GitMachete.EnhancedGraphTable.omitted-branches-text"),
          String.join(", ", skippedBranchNames));
    }

    repaint();
    revalidate();
  }

  @UIEffect
  private void doAutomaticDiscover(Path macheteFilePath) {
    var selectedRepository = getGitRepositorySelectionProvider().getSelectedGitRepository().getOrNull();
    if (selectedRepository == null) {
      LOG.error("Can't do automatic discover because of undefined selected repository");
      return;
    }
    var mainDirPath = GitVfsUtils.getMainDirectoryPath(selectedRepository).toAbsolutePath();
    var gitDirPath = GitVfsUtils.getGitDirectoryPath(selectedRepository).toAbsolutePath();

    new Task.Backgroundable(project, getString("string.GitMachete.EnhancedGraphTable.automatic-discover.task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {
        var discoverRunResult = Try.of(() -> RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
            .getInstance(mainDirPath, gitDirPath).discoverLayoutAndCreateSnapshot());

        if (!discoverRunResult.isSuccess()) {
          var exception = discoverRunResult.getCause();
          GuiUtils.invokeLaterIfNeeded(() -> VcsNotifier.getInstance(project).notifyError(
              getString("string.GitMachete.EnhancedGraphTable.automatic-discover.cant-discover-layout-error-title"),
              exception.getMessage() != null ? exception.getMessage() : ""), NON_MODAL);
          return;
        }

        var repositorySnapshot = discoverRunResult.get();

        if (repositorySnapshot.getRootBranches().size() == 0) {
          GuiUtils.invokeLaterIfNeeded(
              () -> setTextForEmptyTable(format(getString("string.GitMachete.EnhancedGraphTable.empty-machete-file.upper-text"),
                  macheteFilePath.toString())),
              NON_MODAL);
          return;
        }

        var branchLayoutWriter = project.getService(BranchLayoutWriterProvider.class).getBranchLayoutWriter();
        var branchLayout = repositorySnapshot.getBranchLayout().getOrNull();

        if (branchLayout == null) {
          LOG.error("Can't get branch layout from repository snapshot");
          return;
        }

        try {
          branchLayoutWriter.write(macheteFilePath, branchLayout, /* backupOldLayout */ true);

          GuiUtils.invokeLaterIfNeeded(
              () -> {
                gitMacheteRepositorySnapshot = repositorySnapshot;
                var repositoryGraph = repositoryGraphCache.getRepositoryGraph(gitMacheteRepositorySnapshot, isListingCommits);
                setModel(new GraphTableModel(repositoryGraph));
                repaint();
                revalidate();

                VcsNotifier.getInstance(project)
                    .notifyInfo(getString("string.GitMachete.EnhancedGraphTable.automatic-discover.success-message"));
              },
              NON_MODAL);
        } catch (BranchLayoutException exception) {
          GuiUtils.invokeLaterIfNeeded(() -> VcsNotifier.getInstance(project).notifyError(
              getString("string.GitMachete.EnhancedGraphTable.automatic-discover.cant-discover-layout-error-title"),
              exception.getMessage() != null ? exception.getMessage() : ""), NON_MODAL);
        }
      }
    }.queue();
  }

  @Override
  @UIEffect
  public void refreshModel() {
    var gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
    Option<GitRepository> gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (gitRepository.isDefined()) {
      refreshModel(gitRepository.get(), List.empty());
    } else {
      LOG.warn("Selected git repository is undefined; unable to refresh model");
    }
  }

  @UIEffect
  private void initColumns() {
    createDefaultColumnsFromModel();

    // Otherwise sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);
  }

  @Override
  public void queueRepositoryUpdateAndModelRefresh(@UI Runnable doOnUIThreadWhenReady) {
    LOG.debug("Entering");

    if (!project.isDisposed()) {
      GuiUtils.invokeLaterIfNeeded(() -> {
        var gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
        var gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository().getOrNull();
        if (gitRepository == null) {
          LOG.warn("Selected repository is null");
          return;
        }

        @UI Consumer<Option<IGitMacheteRepositorySnapshot>> doRefreshModel = newGitMacheteRepositorySnapshot -> {
          this.gitMacheteRepositorySnapshot = newGitMacheteRepositorySnapshot.getOrNull();
          refreshModel(gitRepository,
              this.gitMacheteRepositorySnapshot != null
                  ? this.gitMacheteRepositorySnapshot.getSkippedBranchNames()
                  : List.empty());
          doOnUIThreadWhenReady.run();
        };

        setTextForEmptyTable(getString("string.GitMachete.EnhancedGraphTable.empty-text"));

        LOG.debug("Queuing repository update onto a non-UI thread");
        new GitMacheteRepositoryUpdateBackgroundable(project, gitRepository, branchLayoutReader, doRefreshModel).queue();
      }, NON_MODAL);
    } else {
      LOG.debug("Project is disposed");
    }
  }

  @Override
  public @Nullable Object getData(String dataId) {
    return Match(dataId).of(
        typeSafeCase(DataKeys.KEY_GIT_MACHETE_REPOSITORY_SNAPSHOT, gitMacheteRepositorySnapshot),
        typeSafeCase(DataKeys.KEY_SELECTED_BRANCH_NAME, selectedBranchName),
        typeSafeCase(CommonDataKeys.PROJECT, project),
        Case($(), (Object) null));
  }

  private static class EnhancedGraphTableMouseAdapter extends MouseAdapter {
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

      // check if we click on one of branches
      if (row < 0 || col < 0) {
        return;
      }

      BranchOrCommitCell cell = (BranchOrCommitCell) graphTable.getModel().getValueAt(row, col);
      IGraphItem graphItem = cell.getGraphItem();
      if (!graphItem.isBranchItem()) {
        return;
      }

      graphTable.selectedBranchName = graphItem.asBranchItem().getBranch().getName();

      ActionManager actionManager = ActionManager.getInstance();
      if (SwingUtilities.isRightMouseButton(e)) {
        ActionGroup contextMenuActionGroup = (ActionGroup) actionManager.getAction(ActionGroupIds.ACTION_GROUP_CONTEXT_MENU);
        var actionPopupMenu = actionManager.createActionPopupMenu(ACTION_PLACE_CONTEXT_MENU, contextMenuActionGroup);
        JPopupMenu popupMenu = actionPopupMenu.getComponent();
        popupMenu.addPopupMenuListener(popupMenuListener);
        popupMenu.show(graphTable, (int) point.getX(), (int) point.getY());
      } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {
        e.consume();
        DataContext dataContext = DataManager.getInstance().getDataContext(graphTable);
        var actionEvent = AnActionEvent.createFromDataContext(ACTION_PLACE_CONTEXT_MENU, new Presentation(), dataContext);
        actionManager.getAction(ACTION_CHECK_OUT).actionPerformed(actionEvent);
      }
    }
  }

  private static class EnhancedGraphTablePopupMenuListener extends PopupMenuListenerAdapter {
    private final EnhancedGraphTable graphTable;

    @UIEffect
    EnhancedGraphTablePopupMenuListener(EnhancedGraphTable graphTable) {
      this.graphTable = graphTable;
    }

    @Override
    @UIEffect
    public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
      // This delay is needed to avoid `focus transfer` effect when at the beginning row selection is light blue
      // but when context menu is created (in a fraction of second), selection loses focus to the context menu and becomes dark blue.
      // TimerTask can't be replaced by lambda because it's not a SAM (single abstract method).
      // For more details see https://stackoverflow.com/a/37970821/10116324
      new Timer().schedule(new TimerTask() {
        @Override
        public void run() {
          GuiUtils.invokeLaterIfNeeded(() -> {
            // So that the selection spans over the full width of the row (for better feeling)
            graphTable.setAutoResizeMode(JBTable.AUTO_RESIZE_ALL_COLUMNS);
            graphTable.setRowSelectionAllowed(true);
          }, NON_MODAL);
        }
      }, /* delay */ 35);
    }

    @Override
    @UIEffect
    public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
      graphTable.setRowSelectionAllowed(false);
      // So that the selection (and thus the area where tooltip is shown) has again the smallest possible size
      graphTable.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
    }
  }
}
