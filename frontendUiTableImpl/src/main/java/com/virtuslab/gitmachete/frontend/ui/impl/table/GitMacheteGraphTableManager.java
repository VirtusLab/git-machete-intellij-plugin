package com.virtuslab.gitmachete.frontend.ui.impl.table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.GuiUtils;
import com.intellij.util.messages.Topic;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.MinLen;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutParserFactory;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.MacheteFileParseException;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphFactory;
import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.api.table.IGraphTableManager;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public final class GitMacheteGraphTableManager implements IGraphTableManager {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendUiTable");

  private final Project project;
  private final IGitRepositorySelectionProvider gitRepositorySelectionProvider;

  @Getter
  @Setter
  private boolean isListingCommits;
  private final AtomicReference<@Nullable IGitMacheteRepository> repositoryRef;
  @Getter
  private final GitMacheteGraphTable graphTable;

  private final IRepositoryGraphFactory repositoryGraphFactory;
  private final IGitMacheteRepositoryFactory gitMacheteRepositoryFactory;
  private final IBranchLayoutParserFactory branchLayoutParserFactory;

  public GitMacheteGraphTableManager(Project project, IGitRepositorySelectionProvider gitRepositorySelectionProvider) {
    this.project = project;
    this.gitRepositorySelectionProvider = gitRepositorySelectionProvider;

    this.isListingCommits = false;
    this.repositoryRef = new AtomicReference<>(null);
    GraphTableModel graphTableModel = new GraphTableModel(IRepositoryGraphFactory.NULL_REPOSITORY_GRAPH);
    this.graphTable = new GitMacheteGraphTable(graphTableModel, project, repositoryRef, gitRepositorySelectionProvider);

    this.branchLayoutParserFactory = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutParserFactory.class);
    this.gitMacheteRepositoryFactory = RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryFactory.class);
    this.repositoryGraphFactory = RuntimeBinding.instantiateSoleImplementingClass(IRepositoryGraphFactory.class);

    // InitializationChecker allows us to invoke instance methods below because the class is final
    // and all fields are already initialized. Hence, `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTableManager.class)`, as would be with a non-final class) at this point.
    subscribeToVcsRootChanges();
    subscribeToGitRepositoryChanges();
  }

  private void subscribeToVcsRootChanges() {
    // The method reference is invoked when user changes repository in combo box menu
    gitRepositorySelectionProvider.addSelectionChangeObserver(this::updateAndRefreshGraphTableInBackground);
  }

  private void subscribeToGitRepositoryChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    GitRepositoryChangeListener listener = repository -> updateAndRefreshGraphTableInBackground();
    project.getMessageBus().connect().subscribe(topic, listener);
  }

  @Override
  @UIEffect
  public void refreshGraphTable() {
    GitRepository gitRepository = gitRepositorySelectionProvider.getSelectedRepository();
    Path macheteFilePath = getMacheteFilePath(gitRepository);
    boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

    refreshGraphTable(macheteFilePath, isMacheteFilePresent);
  }

  /** Creates a new repository graph and sets it to the graph table model. */
  @UIEffect
  private void refreshGraphTable(Path macheteFilePath, boolean isMacheteFilePresent) {
    LOG.debug(() -> "Entering: macheteFilePath = ${macheteFilePath}, isMacheteFilePresent = ${isMacheteFilePresent}");
    // isUnitTestMode() checks if IDEA is running as a command line applet or in unit test mode.
    // No UI should be shown when IDEA is running in this mode.
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.debug("Project is not initialized or application is in unit test mode. Returning.");
      return;
    }

    // TODO (#176): When machete file is not present or it's empty, propose using automatically detected (by discover
    // functionality) branch layout

    IGitMacheteRepository gitMacheteRepository = repositoryRef.get();
    IRepositoryGraph repositoryGraph;
    if (gitMacheteRepository == null) {
      repositoryGraph = IRepositoryGraphFactory.NULL_REPOSITORY_GRAPH;
    } else {
      repositoryGraph = repositoryGraphFactory.getRepositoryGraph(gitMacheteRepository, isListingCommits);
      if (gitMacheteRepository.getRootBranches().isEmpty()) {
        graphTable.setTextForEmptyGraph(
            "Your machete file (${macheteFilePath}) is empty.",
            "Please use 'git machete discover' CLI command to automatically fill in the machete file.");
        LOG.info(() -> "Machete file (${macheteFilePath}) is empty");
      }
    }
    graphTable.getModel().setRepositoryGraph(repositoryGraph);

    if (!isMacheteFilePresent) {
      graphTable.setTextForEmptyGraph(
          "There is no machete file (${macheteFilePath}) for this repository.",
          "Please use 'git machete discover' CLI command to automatically create machete file.");
      LOG.info(() -> "Machete file (${macheteFilePath}) is absent");
    }

    graphTable.repaint();
    graphTable.revalidate();
  }

  private Path getMainDirectoryPath(GitRepository gitRepository) {
    return Paths.get(gitRepository.getRoot().getPath());
  }

  private Path getGitDirectoryPath(GitRepository gitRepository) {
    VirtualFile vfGitDir = GitUtil.findGitDir(gitRepository.getRoot());
    assert vfGitDir != null : "Can't get .git directory from repo root path ${gitRepository.getRoot()}";
    return Paths.get(vfGitDir.getPath());
  }

  private Path getMacheteFilePath(GitRepository gitRepository) {
    return getGitDirectoryPath(gitRepository).resolve("machete");
  }

  @Override
  public void updateAndRefreshGraphTableInBackground() {
    LOG.debug("Entering");

    if (project != null && !project.isDisposed()) {
      LOG.debug("Queuing 'Updating Git Machete repository and refreshing'");
      new Task.Backgroundable(project, "Updating Git Machete repository and refreshing") {
        @Override
        @UIEffect
        public void run(ProgressIndicator indicator) {
          LOG.debug("Updating Git Machete repository and refreshing");

          // GitUtil.getRepositories(project) should never return empty list because it means there is no git repository
          // in an opened project, so Git Machete plugin shouldn't even be loaded in the first place
          @SuppressWarnings("value:assignment.type.incompatible")
          @MinLen(1)
          List<GitRepository> repositories = List.ofAll(GitUtil.getRepositories(project));
          gitRepositorySelectionProvider.updateRepositories(repositories);

          GitRepository gitRepository = gitRepositorySelectionProvider.getSelectedRepository();
          Path mainDirectoryPath = getMainDirectoryPath(gitRepository);
          Path gitDirectoryPath = getGitDirectoryPath(gitRepository);
          Path macheteFilePath = getMacheteFilePath(gitRepository);
          boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

          updateRepository(mainDirectoryPath, gitDirectoryPath, isMacheteFilePresent);
          refreshGraphTable(macheteFilePath, isMacheteFilePresent);
        }
      }.queue();
    } else {
      LOG.debug("project == null or was disposed");
    }
  }

  /**
   * Updates repository which is the base of graph table model. The change will be seen after
   * {@link GitMacheteGraphTableManager#refreshGraphTable()}.
   */
  @SuppressWarnings({"IllegalCatch"})
  private void updateRepository(Path mainDirectoryPath, Path gitDirectoryPath, boolean isMacheteFilePresent) {
    LOG.debug(() -> "Entering: mainDirectoryPath = ${mainDirectoryPath}, gitDirectoryPath = ${gitDirectoryPath}" +
        "isMacheteFilePresent = ${isMacheteFilePresent}");
    if (isMacheteFilePresent) {
      LOG.debug("Machete file is present. Try to create GitMacheteRepository instance");
      // This try-catch is a workaround caused by strange behavior of Try.onFailure() that seems to rethrow exception
      // (or something else happens)
      // For state that caused unexpected behavior see tag `strange-vavr-try-behavior`
      try {
        IBranchLayout branchLayout = updateBranchLayoutAndMacheteFilePath();
        repositoryRef.set(gitMacheteRepositoryFactory.create(mainDirectoryPath, gitDirectoryPath, branchLayout));
      } catch (Exception e) {
        LOG.error("Unable to create Git Machete repository", e);
        String exceptionMessage = e.getMessage();
        VcsNotifier.getInstance(project).notifyError("Repository instantiation failed",
            exceptionMessage != null ? exceptionMessage : "");
        GuiUtils.invokeLaterIfNeeded(
            () -> Messages.showErrorDialog(
                exceptionMessage != null
                    ? exceptionMessage
                    : "Repository instantiation failed. For more information, please look at the IntelliJ logs",
                "Something Went Wrong..."),
            ModalityState.NON_MODAL);
      }
    } else {
      LOG.debug("Machete file is absent. Setting repository reference to null");
      repositoryRef.set(null);
    }
  }

  private IBranchLayout updateBranchLayoutAndMacheteFilePath() throws MacheteFileParseException {
    GitRepository gitRepository = gitRepositorySelectionProvider.getSelectedRepository();
    Path macheteFilePath = getMacheteFilePath(gitRepository);
    IBranchLayout branchLayout = createBranchLayout(macheteFilePath);
    graphTable.setBranchLayout(branchLayout);
    graphTable.setMacheteFilePath(macheteFilePath);
    return branchLayout;
  }

  private IBranchLayout createBranchLayout(Path branchLayoutFilePath) throws MacheteFileParseException {
    return Try.of(() -> branchLayoutParserFactory.create(branchLayoutFilePath).parse())
        .getOrElseThrow(e -> {
          Option<@Positive Integer> errorLine = ((BranchLayoutException) e).getErrorLine();
          return new MacheteFileParseException("Error occurred while parsing machete file ${branchLayoutFilePath}" +
              (errorLine.isDefined() ? " in line ${errorLine.get()}" : ""), e);
        });
  }
}
