package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.ACTION_OPEN_MACHETE_FILE;
import static com.virtuslab.gitmachete.frontend.defs.ActionPlaces.ACTION_PLACE_CONTEXT_MENU;
import static com.virtuslab.gitmachete.frontend.defs.ActionPlaces.ACTION_PLACE_VCS_NOTIFICATION;
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
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.ScrollingUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.JBUI;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
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

  // TODO (#620): rework callbacks into futures
  @UIEffect
  private void refreshModel(
      GitRepository gitRepository,
      Set<String> duplicatedBranchNames,
      Set<String> skippedBranchNames,
      @UI Runnable doOnUIThreadWhenReady) {
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
        if (gitMacheteRepositorySnapshot.getSkippedBranchNames().isEmpty()) {
          LOG.info("Machete file (${macheteFilePath}) is empty, so auto discover is running");
          queueDiscover(macheteFilePath, doOnUIThreadWhenReady);
          return;
        } else {
          setTextForEmptyTable(
              format(getString("string.GitMachete.EnhancedGraphTable.empty-table-text.only-skipped-in-machete-file"),
                  macheteFilePath.toString()));
        }
      }
    }

    if (!isMacheteFilePresent) {
      LOG.info("Machete file (${macheteFilePath}) is absent, so auto discover is running");
      // The `doOnUIThreadWhenReady` callback  must be executed once discover task is *complete*,
      // and not just when the discover task is *enqueued*.
      // Otherwise, it'll most likely happen that the callback executes before the discover task is complete,
      // which is undesirable.
      queueDiscover(macheteFilePath, doOnUIThreadWhenReady);
      return;
    }

    setModel(new GraphTableModel(repositoryGraph));

    if (skippedBranchNames.nonEmpty()) {
      // This warning notification will not cover other error notifications (e.g. when rebase errors occur)
      VcsNotifier.getInstance(project).notifyWarning(
          getString("string.GitMachete.EnhancedGraphTable.skipped-branches-text"),
          String.join(", ", skippedBranchNames));
    }

    if (duplicatedBranchNames.nonEmpty()) {
      // This warning notification will not cover other error notifications (e.g. when rebase errors occur)
      VcsNotifier.getInstance(project).notifyWarning(
          getString("string.GitMachete.EnhancedGraphTable.duplicated-branches-text"),
          String.join(", ", duplicatedBranchNames));
    }

    repaint();
    revalidate();
    doOnUIThreadWhenReady.run();
  }

  public void queueDiscover(Path macheteFilePath, @UI Runnable doOnUIThreadWhenReady) {
    new GitMacheteRepositoryDiscoverer(
        project,
        getGitRepositorySelectionProvider(),
        getUnsuccessfulDiscoverMacheteFilePathConsumer(),
        getSuccessfulDiscoverRepositoryConsumer(doOnUIThreadWhenReady))
            .enqueue(macheteFilePath);
  }

  private Consumer<IGitMacheteRepositorySnapshot> getSuccessfulDiscoverRepositoryConsumer(@UI Runnable doOnUIThreadWhenReady) {
    return (IGitMacheteRepositorySnapshot repositorySnapshot) -> GuiUtils.invokeLaterIfNeeded(
        () -> {
          gitMacheteRepositorySnapshot = repositorySnapshot;
          queueRepositoryUpdateAndModelRefresh(doOnUIThreadWhenReady);

          var notifier = VcsNotifier.getInstance(project);
          var notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
              getString("string.GitMachete.EnhancedGraphTable.automatic-discover.success-message"),
              NotificationType.INFORMATION);
          notification.addAction(NotificationAction.createSimple(
              () -> getString("action.GitMachete.OpenMacheteFileAction.description"), () -> {
                notification.expire();

                DataContext dataContext = DataManager.getInstance().getDataContext(this);
                var actionEvent = AnActionEvent.createFromDataContext(ACTION_PLACE_VCS_NOTIFICATION, new Presentation(),
                    dataContext);
                ActionManager.getInstance().getAction(ACTION_OPEN_MACHETE_FILE).actionPerformed(actionEvent);
              }));
          notifier.notify(notification);
        },
        NON_MODAL);
  }

  private Consumer<Path> getUnsuccessfulDiscoverMacheteFilePathConsumer() {
    return (Path macheteFilePath) -> GuiUtils.invokeLaterIfNeeded(
        () -> setTextForEmptyTable(
            format(getString("string.GitMachete.EnhancedGraphTable.empty-table-text.cannot-discover-layout"),
                macheteFilePath.toString())),
        NON_MODAL);
  }

  @Override
  @UIEffect
  public void refreshModel() {
    var gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
    Option<GitRepository> gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (gitRepository.isDefined()) {
      refreshModel(gitRepository.get(),
          /* duplicatedBranchNames */ TreeSet.empty(),
          /* skippedBranchNames */ TreeSet.empty(),
          /* doOnUIThreadWhenReady */ () -> {});
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
          if (gitMacheteRepositorySnapshot != null) {
            refreshModel(gitRepository,
                this.gitMacheteRepositorySnapshot.getDuplicatedBranchNames(),
                this.gitMacheteRepositorySnapshot.getSkippedBranchNames(),
                doOnUIThreadWhenReady);

          } else {
            refreshModel(gitRepository, TreeSet.empty(), TreeSet.empty(), doOnUIThreadWhenReady);
          }
        };

        setTextForEmptyTable(getString("string.GitMachete.EnhancedGraphTable.empty-table-text.loading"));

        LOG.debug("Queuing repository update onto a non-UI thread");
        new GitMacheteRepositoryUpdateBackgroundable(project, gitRepository, branchLayoutReader, doRefreshModel).queue();

        GitVfsUtils.getMacheteFile(gitRepository).forEach(macheteFile -> VfsUtil.markDirtyAndRefresh(/* async */ true,
            /* recursive */ false, /* reloadChildren */ false, macheteFile));
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
      performActionAfterChecks(e, point);
    }

    @UIEffect
    private void performActionAfterChecks(MouseEvent e, Point point) {
      ActionManager actionManager = ActionManager.getInstance();
      if (SwingUtilities.isRightMouseButton(e)) {
        ActionGroup contextMenuActionGroup = (ActionGroup) actionManager.getAction(ActionGroupIds.ACTION_GROUP_CONTEXT_MENU);
        var actionPopupMenu = actionManager.createActionPopupMenu(ACTION_PLACE_CONTEXT_MENU, contextMenuActionGroup);
        JPopupMenu popupMenu = actionPopupMenu.getComponent();
        popupMenu.addPopupMenuListener(popupMenuListener);
        popupMenu.show(graphTable, (int) point.getX(), (int) point.getY());
      } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {

        var gitMacheteRepositorySnapshot = graphTable.gitMacheteRepositorySnapshot;
        if (gitMacheteRepositorySnapshot != null) {
          var isSelectedEqualToCurrent = gitMacheteRepositorySnapshot
              .getCurrentBranchIfManaged().map(b -> b.getName().equals(graphTable.selectedBranchName)).getOrElse(false);
          if (isSelectedEqualToCurrent) {
            return;
          }
        }

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
          GuiUtils.invokeLaterIfNeeded(() -> graphTable.setRowSelectionAllowed(true), NON_MODAL);
        }
      }, /* delay */ 35);
    }

    @Override
    @UIEffect
    public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
      graphTable.setRowSelectionAllowed(false);
    }
  }
}
