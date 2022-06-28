package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.ACTION_OPEN_MACHETE_FILE;
import static com.virtuslab.gitmachete.frontend.defs.ActionPlaces.ACTION_PLACE_CONTEXT_MENU;
import static com.virtuslab.gitmachete.frontend.defs.ActionPlaces.ACTION_PLACE_VCS_NOTIFICATION;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
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
import com.intellij.notification.Notification;
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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.ScrollingUtil;
import com.intellij.util.ModalityUiUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.JBUI;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.NullGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.defs.ActionGroupIds;
import com.virtuslab.gitmachete.frontend.graph.api.items.IGraphItem;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphCache;
import com.virtuslab.gitmachete.frontend.graph.api.repository.NullRepositoryGraph;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
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
@ExtensionMethod({GitMacheteBundle.class, GitVfsUtils.class})
@CustomLog
public final class EnhancedGraphTable extends BaseEnhancedGraphTable
    implements
      DataProvider,
      IGitMacheteRepositorySnapshotProvider {

  private final Project project;

  private final IBranchLayoutReader branchLayoutReader;
  private final IBranchLayoutWriter branchLayoutWriter;
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
    this.branchLayoutWriter = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);
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

    setDefaultRenderer(BranchOrCommitCell.class, new BranchOrCommitCellRenderer(/* shouldDisplayActionToolTips */ true));

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
    val messageBusConnection = project.getMessageBus().connect();
    messageBusConnection.subscribe(topic, listener);
    Disposer.register(project, messageBusConnection);
  }

  private void subscribeToSelectedGitRepositoryChange() {
    // The method reference is invoked when user changes repository in selection component menu
    val gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
    gitRepositorySelectionProvider.addSelectionChangeObserver(() -> queueRepositoryUpdateAndModelRefresh());
  }

  // TODO (#620): rework callbacks into futures
  @UIEffect
  private void refreshModel(
      GitRepository gitRepository,
      IGitMacheteRepositorySnapshot repositorySnapshot,
      @UI Runnable doOnUIThreadWhenReady) {
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.debug("Project is not initialized or application is in unit test mode. Returning.");
      return;
    }

    // A bit of a shortcut: we're accessing filesystem even though we're on the UI thread here;
    // this shouldn't ever be a heavyweight operation, however.
    Path macheteFilePath = gitRepository.getMacheteFilePath();
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
              getString("string.GitMachete.EnhancedGraphTable.empty-table-text.only-skipped-in-machete-file")
                  .format(macheteFilePath.toString()));
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

    Set<String> skippedBranchNames = repositorySnapshot.getSkippedBranchNames();
    if (skippedBranchNames.nonEmpty()) {
      val notification = getSkippedBranchesNotification(repositorySnapshot, gitRepository);
      VcsNotifier.getInstance(project).notify(notification);
    }

    Set<String> duplicatedBranchNames = repositorySnapshot.getDuplicatedBranchNames();
    if (duplicatedBranchNames.nonEmpty()) {
      // This warning notification will not cover other error notifications (e.g. when rebase errors occur)
      VcsNotifier.getInstance(project).notifyWarning(/* displayId */ null,
          getString("string.GitMachete.EnhancedGraphTable.duplicated-branches-text"),
          String.join(", ", duplicatedBranchNames));
    }

    repaint();
    revalidate();
    doOnUIThreadWhenReady.run();
  }

  private Notification getSkippedBranchesNotification(IGitMacheteRepositorySnapshot repositorySnapshot,
      GitRepository gitRepository) {
    val notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
        getString("string.GitMachete.EnhancedGraphTable.skipped-branches-text")
            .format(String.join(", ", repositorySnapshot.getSkippedBranchNames())),
        NotificationType.WARNING);
    notification.addAction(NotificationAction.createSimple(
        () -> getString("action.GitMachete.EnhancedGraphTable.automatic-discover.slide-out-skipped"), () -> {
          notification.expire();
          slideOutSkippedBranches(repositorySnapshot, gitRepository);
        }));
    notification.addAction(NotificationAction.createSimple(
        () -> getString("action.GitMachete.OpenMacheteFileAction.description"), () -> {
          val actionEvent = createAnActionEvent();
          ActionManager.getInstance().getAction(ACTION_OPEN_MACHETE_FILE).actionPerformed(actionEvent);
        }));
    return notification;
  }

  private void slideOutSkippedBranches(IGitMacheteRepositorySnapshot repositorySnapshot, GitRepository gitRepository) {
    IBranchLayout newBranchLayout = repositorySnapshot.getBranchLayout();
    for (val branchName : repositorySnapshot.getSkippedBranchNames()) {
      newBranchLayout = newBranchLayout.slideOut(branchName);
    }

    try {
      Path macheteFilePath = gitRepository.getMacheteFilePath();
      LOG.info("Writing new branch layout into ${macheteFilePath}");
      branchLayoutWriter.write(macheteFilePath, newBranchLayout, /* backupOldLayout */ true);

    } catch (BranchLayoutException e) {
      String exceptionMessage = e.getMessage();
      String errorMessage = "Error occurred while sliding out skipped branches" +
          (exceptionMessage == null ? "" : ": " + exceptionMessage);
      LOG.error(errorMessage);
      VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
          getString("action.GitMachete.EnhancedGraphTable.branch-layout-write-failure"),
          exceptionMessage == null ? "" : exceptionMessage);
    }
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
    return (IGitMacheteRepositorySnapshot repositorySnapshot) -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> {
      gitMacheteRepositorySnapshot = repositorySnapshot;
      queueRepositoryUpdateAndModelRefresh(doOnUIThreadWhenReady);

      val notifier = VcsNotifier.getInstance(project);
      val notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
          getString("string.GitMachete.EnhancedGraphTable.automatic-discover.success-message"),
          NotificationType.INFORMATION);
      notification.addAction(NotificationAction.createSimple(
          () -> getString("action.GitMachete.OpenMacheteFileAction.description"), () -> {
            val actionEvent = createAnActionEvent();
            ActionManager.getInstance().getAction(ACTION_OPEN_MACHETE_FILE).actionPerformed(actionEvent);
          }));
      notifier.notify(notification);
    });
  }

  private AnActionEvent createAnActionEvent() {
    val dataContext = DataManager.getInstance().getDataContext(this);
    return AnActionEvent.createFromDataContext(ACTION_PLACE_VCS_NOTIFICATION, new Presentation(), dataContext);
  }

  private Consumer<Path> getUnsuccessfulDiscoverMacheteFilePathConsumer() {
    return (Path macheteFilePath) -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> setTextForEmptyTable(
        getString("string.GitMachete.EnhancedGraphTable.empty-table-text.cannot-discover-layout")
            .format(macheteFilePath.toString())));
  }

  @Override
  @UIEffect
  public void refreshModel() {
    val gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
    Option<GitRepository> gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (gitRepository.isDefined()) {
      refreshModel(gitRepository.get(),
          NullGitMacheteRepositorySnapshot.getInstance(),
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
      /* async */
      /* recursive */
      /* reloadChildren */
      ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> {
        val gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
        val gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository().getOrNull();
        if (gitRepository == null) {
          LOG.warn("Selected repository is null");
          return;
        }

        @UI Consumer<Option<IGitMacheteRepositorySnapshot>> doRefreshModel = newGitMacheteRepositorySnapshot -> {
          val nullableRepositorySnapshot = newGitMacheteRepositorySnapshot.getOrNull();
          this.gitMacheteRepositorySnapshot = nullableRepositorySnapshot;
          if (nullableRepositorySnapshot != null) {
            refreshModel(gitRepository,
                nullableRepositorySnapshot,
                doOnUIThreadWhenReady);

          } else {
            refreshModel(gitRepository, NullGitMacheteRepositorySnapshot.getInstance(), doOnUIThreadWhenReady);
          }
        };

        setTextForEmptyTable(getString("string.GitMachete.EnhancedGraphTable.empty-table-text.loading"));

        LOG.debug("Queuing repository update onto a non-UI thread");
        new GitMacheteRepositoryUpdateBackgroundable(project, gitRepository, branchLayoutReader, doRefreshModel).queue();

        gitRepository.getMacheteFile().forEach(macheteFile -> VfsUtil.markDirtyAndRefresh(/* async */ true,
            /* recursive */ false, /* reloadChildren */ false, macheteFile));
      });
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
        val actionPopupMenu = actionManager.createActionPopupMenu(ACTION_PLACE_CONTEXT_MENU, contextMenuActionGroup);
        JPopupMenu popupMenu = actionPopupMenu.getComponent();
        popupMenu.addPopupMenuListener(popupMenuListener);
        popupMenu.show(graphTable, (int) point.getX(), (int) point.getY());
      } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {

        val gitMacheteRepositorySnapshot = graphTable.gitMacheteRepositorySnapshot;
        if (gitMacheteRepositorySnapshot != null) {
          val isSelectedEqualToCurrent = gitMacheteRepositorySnapshot
              .getCurrentBranchIfManaged().map(b -> b.getName().equals(graphTable.selectedBranchName)).getOrElse(false);
          if (isSelectedEqualToCurrent) {
            return;
          }
        }

        e.consume();
        DataContext dataContext = DataManager.getInstance().getDataContext(graphTable);
        val actionEvent = AnActionEvent.createFromDataContext(ACTION_PLACE_CONTEXT_MENU, new Presentation(), dataContext);
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
          ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> graphTable.setRowSelectionAllowed(true));
        }
      }, /* delay in ms */ 35);
    }

    @Override
    @UIEffect
    public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
      graphTable.setRowSelectionAllowed(false);
    }
  }
}
