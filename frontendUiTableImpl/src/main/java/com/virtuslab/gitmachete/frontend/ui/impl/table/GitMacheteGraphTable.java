package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.datakeys.DataKeys.typeSafeCase;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.frontend.defs.ActionIds.ACTION_OPEN_MACHETE_FILE;
import static com.virtuslab.gitmachete.frontend.defs.ActionPlaces.ACTION_PLACE_CONTEXT_MENU;
import static com.virtuslab.gitmachete.frontend.defs.ActionPlaces.ACTION_PLACE_EMPTY_TABLE;
import static com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils.getMacheteFilePath;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

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
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.JBUI;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.collection.List;
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
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.impl.cell.BranchOrCommitCellRendererComponent;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;

// TODO (#99): consider applying SpeedSearch for branches and commits
@CustomLog
public final class GitMacheteGraphTable extends BaseGraphTable implements DataProvider, IGitMacheteRepositorySnapshotProvider {

  private final Project project;

  private final IBranchLayoutReader branchLayoutReader;
  private final IRepositoryGraphCache repositoryGraphCache;

  @Getter
  @Setter
  @UIEffect
  private boolean isListingCommits;

  @UIEffect
  @Getter
  private @Nullable IGitMacheteRepositorySnapshot gitMacheteRepositorySnapshot;

  @UIEffect
  private @Nullable String selectedBranchName;

  @UIEffect
  public GitMacheteGraphTable(Project project) {
    super(new GraphTableModel(NullRepositoryGraph.getInstance()));

    this.project = project;
    this.branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
    this.repositoryGraphCache = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphCache.class);
    this.isListingCommits = false;

    // InitializationChecker allows us to invoke the below methods because the class is final
    // and all `@NonNull` fields are already initialized. `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTable.class)`, as would be with a non-final class) at this point.

    initColumns();

    setDefaultRenderer(BranchOrCommitCell.class, BranchOrCommitCellRendererComponent::new);

    setCellSelectionEnabled(false);
    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(/* table */ this, /* cycleScrolling */ false);

    addMouseListener(new GitMacheteGraphTableMouseAdapter( /* outer */ this));

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
    // TODO (#176): When machete file is absent or empty,
    // propose using branch layout automatically detected by discover functionality
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
        setTextForEmptyTable(
            /* upperText */ "Provided machete file (${macheteFilePath}) is empty.",
            /* lowerText */ "Open machete file",
            /* onClickRunnableAction */ () -> invokeOpenMacheteFileAction());
        LOG.info("Machete file (${macheteFilePath}) is empty");
      }
    }

    setModel(new GraphTableModel(repositoryGraph));

    if (!isMacheteFilePresent) {
      setTextForEmptyTable(
          "There is no machete file (${macheteFilePath}) for this repository.",
          "Create & open machete file", () -> invokeOpenMacheteFileAction());
      LOG.info("Machete file (${macheteFilePath}) is absent");
    }

    if (skippedBranchNames.nonEmpty()) {
      // This warning notification will not cover other error notifications (e.g. when rebase errors occur)
      VcsNotifier.getInstance(project).notifyWarning(
          "The following branches defined by machete file do not belong to the local repository:",
          String.join(", ", skippedBranchNames));
    }

    repaint();
    revalidate();
  }

  @UIEffect
  private void invokeOpenMacheteFileAction() {
    var action = ActionManager.getInstance().getAction(ACTION_OPEN_MACHETE_FILE);
    var dataContext = DataManager.getInstance().getDataContext(GitMacheteGraphTable.this);
    var anActionEvent = AnActionEvent.createFromDataContext(ACTION_PLACE_EMPTY_TABLE, new Presentation(), dataContext);
    GuiUtils.invokeLaterIfNeeded(() -> action.actionPerformed(anActionEvent), NON_MODAL);
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
  private void setTextForEmptyTable(String upperText, @Nullable String lowerText, @Nullable @UI Runnable onClickRunnable) {
    var attrs = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.Link.linkColor());
    var statusText = getEmptyText().setText(upperText);
    if (lowerText != null) {
      statusText.appendSecondaryText(lowerText, attrs,
          /* listener */ onClickRunnable != null ? __ -> onClickRunnable.run() : null);
    }
  }

  @UIEffect
  private void setTextForEmptyTable(String upperText) {
    setTextForEmptyTable(upperText, /* lowerText */ null, /* onClickRunnable */ null);
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

        @UI Consumer<Option<IGitMacheteRepositorySnapshot>> doRefreshModel = newGitMacheteRepository -> {
          this.gitMacheteRepositorySnapshot = newGitMacheteRepository.getOrNull();
          refreshModel(gitRepository,
              this.gitMacheteRepositorySnapshot != null
                  ? this.gitMacheteRepositorySnapshot.getSkippedBranchNames()
                  : List.empty());
          doOnUIThreadWhenReady.run();
        };

        setTextForEmptyTable("Loading...");

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

  private static class GitMacheteGraphTableMouseAdapter extends MouseAdapter {
    private final GitMacheteGraphTable outer;

    @UIEffect
    GitMacheteGraphTableMouseAdapter(GitMacheteGraphTable outer) {
      this.outer = outer;
    }

    @Override
    @UIEffect
    public void mouseClicked(MouseEvent e) {
      Point point = e.getPoint();
      int row = outer.rowAtPoint(point);
      int col = outer.columnAtPoint(point);

      // check if we click on one of branches
      if (row < 0 || col < 0) {
        return;
      }

      BranchOrCommitCell cell = (BranchOrCommitCell) outer.getModel().getValueAt(row, col);
      IGraphItem graphItem = cell.getGraphItem();
      if (!graphItem.isBranchItem()) {
        return;
      }

      outer.selectedBranchName = graphItem.asBranchItem().getBranch().getName();

      ActionManager actionManager = ActionManager.getInstance();
      if (SwingUtilities.isRightMouseButton(e)) {
        ActionGroup contextMenuActionGroup = (ActionGroup) actionManager.getAction(ActionGroupIds.ACTION_GROUP_CONTEXT_MENU);
        var actionPopupMenu = actionManager.createActionPopupMenu(ACTION_PLACE_CONTEXT_MENU, contextMenuActionGroup);
        actionPopupMenu.getComponent().show(outer, (int) point.getX(), (int) point.getY());
      } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {
        e.consume();
        DataContext dataContext = DataManager.getInstance().getDataContext(outer);
        var actionEvent = AnActionEvent.createFromDataContext(ACTION_PLACE_CONTEXT_MENU, new Presentation(), dataContext);
        actionManager.getAction(ACTION_CHECK_OUT).actionPerformed(actionEvent);
      }
    }
  }

}
