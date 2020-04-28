package com.virtuslab.gitmachete.frontend.ui.table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
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
import com.intellij.util.SmartList;
import com.intellij.util.messages.Topic;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.MinLen;
import org.reflections.Reflections;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutParserFactory;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.backend.api.MacheteFileParseException;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraphFactory;
import com.virtuslab.gitmachete.frontend.ui.VcsRootComboBox;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public final class GitMacheteGraphTableManager {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendUiTable");

  private final Project project;
  @Getter
  @Setter
  private boolean isListingCommits;
  @Getter
  private final GitMacheteGraphTable gitMacheteGraphTable;
  private final AtomicReference<@Nullable IGitMacheteRepository> repositoryRef = new AtomicReference<>(null);
  private final RepositoryGraphFactory repositoryGraphFactory;
  private final VcsRootComboBox vcsRootComboBox;
  private final IGitMacheteRepositoryFactory gitMacheteRepositoryFactory;
  private final IBranchLayoutParserFactory branchLayoutParserFactory;

  public GitMacheteGraphTableManager(Project project, VcsRootComboBox vcsRootComboBox) {
    this.project = project;
    this.isListingCommits = false;
    GraphTableModel graphTableModel = new GraphTableModel(RepositoryGraphFactory.getNullRepositoryGraph());
    this.gitMacheteGraphTable = new GitMacheteGraphTable(graphTableModel, project, repositoryRef,
        vcsRootComboBox);
    this.repositoryGraphFactory = new RepositoryGraphFactory();
    this.vcsRootComboBox = vcsRootComboBox;
    this.gitMacheteRepositoryFactory = getFactoryInstance(IGitMacheteRepositoryFactory.class);
    this.branchLayoutParserFactory = getFactoryInstance(IBranchLayoutParserFactory.class);

    // InitializationChecker allows us to invoke instance methods below because the class is final
    // and all fields are already initialized. Hence, `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTableManager.class)`, as would be with a non-final class) at this point.
    subscribeToVcsRootChanges();
    subscribeToGitRepositoryChanges();
  }
  @SneakyThrows
  private static <T> T getFactoryInstance(Class<T> clazz) {
    Reflections reflections = new Reflections("com.virtuslab");
    Set<Class<? extends T>> classes = reflections.getSubTypesOf(clazz);
    return classes.iterator().next().getDeclaredConstructor().newInstance();
  }

  private void subscribeToVcsRootChanges() {
    // The method reference is invoked when user changes repository in combo box menu
    vcsRootComboBox.addObserver(this::updateAndRefreshInBackground);
  }

  private void subscribeToGitRepositoryChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    GitRepositoryChangeListener listener = repository -> updateAndRefreshInBackground();
    project.getMessageBus().connect().subscribe(topic, listener);
  }

  @UIEffect
  public void refreshGraphTable() {
    GitRepository gitRepository = vcsRootComboBox.getValue();
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
    RepositoryGraph repositoryGraph;
    if (gitMacheteRepository == null) {
      repositoryGraph = RepositoryGraph.getNullRepositoryGraph();
    } else {
      repositoryGraph = repositoryGraphFactory.getRepositoryGraph(gitMacheteRepository, isListingCommits);
      if (gitMacheteRepository.getRootBranches().isEmpty()) {
        gitMacheteGraphTable.setTextForEmptyGraph(
            "Your machete file is empty.",
            "Please use 'git machete discover' CLI command to automatically fill in the machete file.");
        LOG.info(() -> "Machete file (${macheteFilePath}) is empty");
      }
    }
    gitMacheteGraphTable.getModel().setRepositoryGraph(repositoryGraph);

    if (!isMacheteFilePresent) {
      gitMacheteGraphTable.setTextForEmptyGraph(
          "There is no machete file (${macheteFilePath}) for this repository.",
          "Please use 'git machete discover' CLI command to automatically create machete file.");
      LOG.info(() -> "Machete file (${macheteFilePath}) is absent");
    }

    gitMacheteGraphTable.repaint();
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

  public void updateAndRefreshInBackground() {
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
          List<GitRepository> repositories = new SmartList<>(GitUtil.getRepositories(project));
          vcsRootComboBox.updateRepositories(repositories);

          GitRepository gitRepository = vcsRootComboBox.getValue();
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
    GitRepository gitRepository = vcsRootComboBox.getValue();
    Path macheteFilePath = getMacheteFilePath(gitRepository);
    IBranchLayout branchLayout = createBranchLayout(macheteFilePath);
    gitMacheteGraphTable.setBranchLayout(branchLayout);
    gitMacheteGraphTable.setMacheteFilePath(macheteFilePath);
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
