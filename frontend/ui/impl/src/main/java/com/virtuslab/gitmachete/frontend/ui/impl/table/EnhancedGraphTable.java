package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.runWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.OPEN_MACHETE_FILE;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.ListSelectionModel;

import com.intellij.ide.DataManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.ui.ScrollingUtil;
import com.intellij.util.ModalityUiUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.JBUI;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.ILocalBranchReference;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.api.NullGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.gitmachete.frontend.defs.FileTypeIds;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphCache;
import com.virtuslab.gitmachete.frontend.graph.api.repository.NullRepositoryGraph;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCellRenderer;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.async.DoesNotContinueInBackground;
import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;

/**
 *  This class compared to {@link SimpleGraphTable} has graph table refreshing and provides
 *  data like last clicked branch name, opened project or {@link IGitMacheteRepositorySnapshot} of current
 *  repository for actions.
 */
@ExtensionMethod({GitMacheteBundle.class, GitVfsUtils.class})
@CustomLog
public final class EnhancedGraphTable extends BaseEnhancedGraphTable
    implements
      DataProvider,
      Disposable {

  @Getter(AccessLevel.PACKAGE)
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

  @Getter(AccessLevel.PACKAGE)
  @Setter(AccessLevel.PACKAGE)
  @UIEffect
  private @Nullable String selectedBranchName;

  // To accurately track the change of the current branch from the beginning, let's put something impossible
  // as a branch name. This is required to detect the moment when the unmanaged branch notification should be shown.
  @UIEffect
  private String mostRecentlyCheckedOutBranch = "?!@#$%^&";

  @UIEffect
  private @MonotonicNonNull UnmanagedBranchNotification unmanagedBranchNotification;

  private final AtomicReference<@Nullable IGitMacheteRepository> gitMacheteRepositoryRef = new AtomicReference<>(null);

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
    subscribeToMacheteFileChange();
  }

  /**
   * This function is provided only for use with UiTests, to ensure that VFS notices
   * the external change to the machete file made by the test.
   */
  public void refreshMacheteFile() {
    val gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
    val gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (gitRepository != null) {
      Path macheteFilePath = gitRepository.getMacheteFilePath();
      val macheteVFile = VirtualFileManager.getInstance().findFileByNioPath(macheteFilePath);
      if (macheteVFile != null) {
        macheteVFile.refresh(/* asynchronous */ false, /* recursive */ false);
      }
    }
  }

  private IGitRepositorySelectionProvider getGitRepositorySelectionProvider() {
    return project.getService(IGitRepositorySelectionProvider.class);
  }

  private void subscribeToMacheteFileChange() {
    val messageBusConnection = project.getMessageBus().connect();
    messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {

      @Override
      @UIEffect
      public void after(java.util.List<? extends VFileEvent> events) {
        for (val event : events) {
          if (event instanceof VFileContentChangeEvent) {
            VirtualFile file = ((VFileContentChangeEvent) event).getFile();
            if (file.getFileType().getName().equals(FileTypeIds.NAME)) {
              if (unmanagedBranchNotification != null && !unmanagedBranchNotification.isExpired()) {
                unmanagedBranchNotification.expire();
              }
              queueRepositoryUpdateAndModelRefresh();
            }
          }
        }
      }

    });
    Disposer.register(this, messageBusConnection);
  }

  @DoesNotContinueInBackground(reason = "because the call to trackCurrentBranchChange happens in listener")
  @UIEffect
  private void subscribeToGitRepositoryFilesChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    // Let's explicitly mark this listener as @AlwaysSafe
    // as we've checked experimentally that there is no guarantee that it'll run on UI thread.
    @AlwaysSafe GitRepositoryChangeListener listener = repository -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL,
        () -> trackCurrentBranchChange(repository));

    val messageBusConnection = project.getMessageBus().connect();
    messageBusConnection.subscribe(topic, listener);
    Disposer.register(this, messageBusConnection);
  }

  @ContinuesInBackground
  @IgnoreUIThreadUnsafeCalls("com.virtuslab.gitmachete.backend.api.IGitMacheteRepository.inferParentForLocalBranch"
      + "(io.vavr.collection.Set, java.lang.String)")
  private void inferParentForUnmanagedBranchNotificationAndNotify(String branchName) {
    val gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
    val gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (gitRepository == null) {
      LOG.warn("Selected repository is null");
      return;
    }

    Path macheteFilePath = gitRepository.getMacheteFilePath();
    val macheteVFile = VirtualFileManager.getInstance().findFileByNioPath(macheteFilePath);
    boolean isMacheteFilePresent = macheteVFile != null && !macheteVFile.isDirectory();

    if (!isMacheteFilePresent) {
      LOG.warn("Machete file (${macheteFilePath}) is absent, so no unmanaged branch notification will show up");
      return;
    }

    val repository = gitMacheteRepositoryRef.get();
    if (repository == null) {
      LOG.warn("gitMacheteRepository is null, so no unmanaged branch notification will show up");
      return;
    }

    if (gitMacheteRepositorySnapshot == null) {
      LOG.warn("gitMacheteRepositorySnapshot is null, so no unmanaged branch notification will show up");
      return;
    }

    val eligibleLocalBranchNames = gitMacheteRepositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName)
        .toSet();

    new SlideInUnmanagedBranch(
        project,
        /* inferParentResultSupplier */() -> Try
            .of(() -> repository.inferParentForLocalBranch(eligibleLocalBranchNames, branchName)),
        /* gitRepositorySelectionProvider */ getGitRepositorySelectionProvider(),
        /* onSuccessInferredParentBranchConsumer */ inferredParent -> notifyAboutUnmanagedBranch(inferredParent, branchName))
            .enqueue();
  }

  private void notifyAboutUnmanagedBranch(ILocalBranchReference inferredParent, String branchName) {
    ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> {
      val showForThisProject = UnmanagedBranchNotificationFactory.shouldShowForThisProject(project);
      val showForThisBranch = UnmanagedBranchNotificationFactory.shouldShowForThisBranch(project, branchName);
      if (showForThisProject && showForThisBranch) {
        val notification = new UnmanagedBranchNotificationFactory(project, gitMacheteRepositorySnapshot, branchName,
            inferredParent).create();
        VcsNotifier.getInstance(project).notify(notification);
        unmanagedBranchNotification = notification;
      }
    });
  }

  // This method can only be executed on UI thread since it writes certain field(s)
  // which we only ever want to write on UI thread to avoid race conditions.
  @ContinuesInBackground
  @UIEffect
  private void trackCurrentBranchChange(GitRepository repository) {
    val repositoryCurrentBranch = repository.getCurrentBranch();
    if (repositoryCurrentBranch != null) {
      val repositoryCurrentBranchName = repositoryCurrentBranch.getName();
      if (!repositoryCurrentBranchName.equals(mostRecentlyCheckedOutBranch)) {
        if (unmanagedBranchNotification != null) {
          unmanagedBranchNotification.expire();
        }
        if (gitMacheteRepositorySnapshot != null) {
          val entry = gitMacheteRepositorySnapshot.getBranchLayout().getEntryByName(repositoryCurrentBranchName);
          if (entry == null) {
            inferParentForUnmanagedBranchNotificationAndNotify(repositoryCurrentBranchName);
          }
        }
        mostRecentlyCheckedOutBranch = repositoryCurrentBranchName;
      }
    }
    queueRepositoryUpdateAndModelRefresh();
  }

  private void subscribeToSelectedGitRepositoryChange() {
    // The method reference is invoked when user changes repository in the selection component menu
    val gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
    gitRepositorySelectionProvider.addSelectionChangeObserver(() -> queueRepositoryUpdateAndModelRefresh());
  }

  @ContinuesInBackground
  @UIEffect
  private void refreshModel(
      GitRepository gitRepository,
      IGitMacheteRepositorySnapshot repositorySnapshot,
      @UI Runnable doOnUIThreadWhenReady) {
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.debug("Project is not initialized or application is in unit test mode. Returning.");
      return;
    }

    Path macheteFilePath = gitRepository.getMacheteFilePath();
    val macheteVFile = VirtualFileManager.getInstance().findFileByNioPath(macheteFilePath);
    boolean isMacheteFilePresent = macheteVFile != null && !macheteVFile.isDirectory();

    LOG.debug(() -> "Entering: macheteFilePath = ${macheteFilePath}, isMacheteFilePresent = ${isMacheteFilePresent}, " +
        "isListingCommits = ${isListingCommits}");

    IRepositoryGraph repositoryGraph;
    if (gitMacheteRepositorySnapshot == null) {
      repositoryGraph = NullRepositoryGraph.getInstance();
    } else {
      repositoryGraph = repositoryGraphCache.getRepositoryGraph(gitMacheteRepositorySnapshot, isListingCommits);
      if (gitMacheteRepositorySnapshot.getRootBranches().isEmpty()) {
        if (gitMacheteRepositorySnapshot.getSkippedBranchNames().isEmpty()) {
          LOG.info("Machete file (${macheteFilePath}) is empty");
          setTextForEmptyTable(
              getString("string.GitMachete.EnhancedGraphTable.empty-table-text.try-running-discover")
                  .fmt(macheteFilePath.toString()));
          return;
        } else {
          setTextForEmptyTable(
              getString("string.GitMachete.EnhancedGraphTable.empty-table-text.only-skipped-in-machete-file")
                  .fmt(macheteFilePath.toString()));
        }
      }
    }

    if (!isMacheteFilePresent) {
      LOG.info("Machete file (${macheteFilePath}) is absent, so auto discover is running");
      // The `doOnUIThreadWhenReady` callback must be executed once the discover task is *complete*,
      // and not just when the discover task is *enqueued*.
      // Otherwise, it'll most likely happen that the callback executes before the discover task is complete,
      // which is undesirable.
      queueDiscover(macheteFilePath, doOnUIThreadWhenReady);
      return;
    }

    setModel(new GraphTableModel(repositoryGraph));

    if (!macheteFileIsOpenedAndFocused(macheteFilePath)) {
      // notify if a branch listed in the machete file does not exist
      Set<String> skippedBranchNames = repositorySnapshot.getSkippedBranchNames();
      if (skippedBranchNames.nonEmpty()) {
        val notification = getSkippedBranchesNotification(repositorySnapshot, gitRepository);
        VcsNotifier.getInstance(project).notify(notification);
      }

      // notify if a branch name listed in the machete file appears more than once
      Set<String> duplicatedBranchNames = repositorySnapshot.getDuplicatedBranchNames();
      if (duplicatedBranchNames.nonEmpty()) {
        // This warning notification will not cover other error notifications (e.g. when rebase errors occur)
        VcsNotifier.getInstance(project).notifyWarning(/* displayId */ null,
            getString("string.GitMachete.EnhancedGraphTable.duplicated-branches-text"),
            String.join(", ", duplicatedBranchNames));
      }
    }

    repaint();
    revalidate();
    doOnUIThreadWhenReady.run();
  }

  @UIEffect
  private boolean macheteFileIsOpenedAndFocused(Path macheteFilePath) {
    val fileEditorManager = FileEditorManager.getInstance(project);
    val macheteVirtualFile = List.of(fileEditorManager.getSelectedFiles())
        .find(virtualFile -> virtualFile.getPath().equals(macheteFilePath.toString()));
    if (macheteVirtualFile.isEmpty()) {
      return false;
    } else {
      return fileEditorManager.getAllEditors(macheteVirtualFile.get()).length > 0;
    }
  }

  private Notification getSkippedBranchesNotification(IGitMacheteRepositorySnapshot repositorySnapshot,
      GitRepository gitRepository) {
    val notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
        getString("string.GitMachete.EnhancedGraphTable.skipped-branches-text")
            .fmt(String.join(", ", repositorySnapshot.getSkippedBranchNames())),
        NotificationType.WARNING);

    notification.addAction(NotificationAction.createSimple(
        getString("action.GitMachete.EnhancedGraphTable.automatic-discover.slide-out-skipped"), () -> {
          notification.expire();
          slideOutSkippedBranches(repositorySnapshot, gitRepository);
        }));
    notification.addAction(NotificationAction.createSimple(
        getString("action.GitMachete.OpenMacheteFileAction.description"), () -> {
          val actionEvent = createAnActionEvent();
          ActionManager.getInstance().getAction(OPEN_MACHETE_FILE).actionPerformed(actionEvent);
        }));
    return notification;
  }

  private void slideOutSkippedBranches(IGitMacheteRepositorySnapshot repositorySnapshot, GitRepository gitRepository) {
    BranchLayout newBranchLayout = repositorySnapshot.getBranchLayout();
    for (val branchName : repositorySnapshot.getSkippedBranchNames()) {
      newBranchLayout = newBranchLayout.slideOut(branchName);
    }

    val finalNewBranchLayout = newBranchLayout;
    runWriteActionOnUIThread(() -> {
      try {
        Path macheteFilePath = gitRepository.getMacheteFilePath();
        LOG.info("Writing new branch layout into ${macheteFilePath}");
        MacheteFileWriter.writeBranchLayout(
            macheteFilePath,
            branchLayoutWriter,
            finalNewBranchLayout,
            /* backupOldLayout */ true,
            /* requestor */ this);
      } catch (IOException t) {
        String exceptionMessage = t.getMessage();
        String errorMessage = "Error occurred while sliding out skipped branches" +
            (exceptionMessage == null ? "" : ": " + exceptionMessage);
        LOG.error(errorMessage);
        VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
            getString("action.GitMachete.EnhancedGraphTable.branch-layout-write-failure"),
            exceptionMessage == null ? "" : exceptionMessage);
      }
    });
  }

  @ContinuesInBackground
  public void queueDiscover(Path macheteFilePath, @UI Runnable doOnUIThreadWhenReady) {
    new GitMacheteRepositoryDiscoverer(
        project,
        getGitRepositorySelectionProvider(),
        getUnsuccessfulDiscoverMacheteFilePathConsumer(),
        getSuccessfulDiscoverRepositorySnapshotConsumer(doOnUIThreadWhenReady),
        /* getSuccessfulDiscoverRepositoryConsumer */ gitMacheteRepositoryRef::set)
            .enqueue(macheteFilePath);
  }

  @ContinuesInBackground
  private Consumer<IGitMacheteRepositorySnapshot> getSuccessfulDiscoverRepositorySnapshotConsumer(
      @UI Runnable doOnUIThreadWhenReady) {
    return (IGitMacheteRepositorySnapshot repositorySnapshot) -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> {
      gitMacheteRepositorySnapshot = repositorySnapshot;
      queueRepositoryUpdateAndModelRefresh(doOnUIThreadWhenReady);

      val notifier = VcsNotifier.getInstance(project);
      val notification = VcsNotifier.STANDARD_NOTIFICATION.createNotification(
          getString("string.GitMachete.EnhancedGraphTable.automatic-discover.success-message"),
          NotificationType.INFORMATION);
      notification.addAction(NotificationAction.createSimple(
          getString("action.GitMachete.OpenMacheteFileAction.description"), () -> {
            val actionEvent = createAnActionEvent();
            ActionManager.getInstance().getAction(OPEN_MACHETE_FILE).actionPerformed(actionEvent);
          }));
      notifier.notify(notification);
    });
  }

  private AnActionEvent createAnActionEvent() {
    val dataContext = DataManager.getInstance().getDataContext(this);
    return AnActionEvent.createFromDataContext(ActionPlaces.VCS_NOTIFICATION, new Presentation(), dataContext);
  }

  private Consumer<Path> getUnsuccessfulDiscoverMacheteFilePathConsumer() {
    return (Path macheteFilePath) -> ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> setTextForEmptyTable(
        getString("string.GitMachete.EnhancedGraphTable.empty-table-text.cannot-discover-layout")
            .fmt(macheteFilePath.toString())));
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void refreshModel() {
    val gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
    val gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (gitRepository != null) {
      refreshModel(gitRepository,
          NullGitMacheteRepositorySnapshot.getInstance(),
          /* doOnUIThreadWhenReady */ () -> {});
    } else {
      LOG.warn("Selected git repository is undefined; unable to refresh model");
    }
  }

  @UIEffect
  private void initColumns() {
    createDefaultColumnsFromModel();

    // Otherwise, sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);
  }

  @ContinuesInBackground
  @Override
  public void queueRepositoryUpdateAndModelRefresh(@UI Runnable doOnUIThreadWhenReady) {
    LOG.debug("Entering");

    if (!project.isDisposed()) {
      ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> {
        val gitRepositorySelectionProvider = getGitRepositorySelectionProvider();
        val gitRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
        if (gitRepository == null) {
          LOG.warn("Selected repository is null");
          return;
        }

        @UI Consumer<@Nullable IGitMacheteRepositorySnapshot> doRefreshModel = newGitMacheteRepositorySnapshot -> {
          this.gitMacheteRepositorySnapshot = newGitMacheteRepositorySnapshot;
          if (newGitMacheteRepositorySnapshot != null) {
            validateUnmanagedBranchNotification(newGitMacheteRepositorySnapshot, unmanagedBranchNotification);
            refreshModel(gitRepository, newGitMacheteRepositorySnapshot, doOnUIThreadWhenReady);

          } else {
            refreshModel(gitRepository, NullGitMacheteRepositorySnapshot.getInstance(), doOnUIThreadWhenReady);
          }
        };

        setTextForEmptyTable(getString("string.GitMachete.EnhancedGraphTable.empty-table-text.loading"));

        LOG.debug("Queuing repository update onto a non-UI thread");
        new GitMacheteRepositoryUpdateBackgroundable(
            gitRepository,
            branchLayoutReader,
            doRefreshModel,
            /* gitMacheteRepositoryConsumer */ gitMacheteRepositoryRef::set).queue();

        val macheteFile = gitRepository.getMacheteFile();

        if (macheteFile != null) {
          VfsUtil.markDirtyAndRefresh(/* async */ true, /* recursive */ false, /* reloadChildren */ false, macheteFile);
        }
      });
    } else {
      LOG.debug("Project is disposed");
    }
  }

  @UIEffect
  private static void validateUnmanagedBranchNotification(IGitMacheteRepositorySnapshot newGitMacheteRepositorySnapshot,
      @Nullable UnmanagedBranchNotification notification) {
    val branchEntryExists = Option.of(notification)
        .map(UnmanagedBranchNotification::getBranchName)
        .flatMap(b -> Option.of(newGitMacheteRepositorySnapshot.getBranchLayout().getEntryByName(b)))
        .isDefined();
    if (branchEntryExists) {
      assert notification != null : "unmanagedBranchNotification is null";
      notification.expire();
      val balloon = notification.getBalloon();
      if (balloon != null) {
        balloon.hide();
      }
    }
  }

  @Override
  public @Nullable Object getData(String dataId) {
    return Match(dataId).of(
        typeSafeCase(DataKeys.GIT_MACHETE_REPOSITORY_SNAPSHOT, gitMacheteRepositorySnapshot),
        typeSafeCase(DataKeys.SELECTED_BRANCH_NAME, selectedBranchName),
        typeSafeCase(CommonDataKeys.PROJECT, project),
        Case($(), (Object) null));
  }

  @Override
  public void dispose() {

  }
}
