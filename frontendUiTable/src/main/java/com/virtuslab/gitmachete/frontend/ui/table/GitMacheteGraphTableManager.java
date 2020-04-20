package com.virtuslab.gitmachete.frontend.ui.table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.GuiUtils;
import com.intellij.util.messages.Topic;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reflections.Reflections;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraphFactory;
import com.virtuslab.gitmachete.frontend.ui.selection.ISelectionChangeObservable;

public final class GitMacheteGraphTableManager {
  private static final Logger LOG = Logger.getInstance(GitMacheteGraphTableManager.class);

  private final Project project;
  @Getter
  @Setter
  private boolean isListingCommits;
  @Getter
  private final GitMacheteGraphTable gitMacheteGraphTable;
  private final AtomicReference<@Nullable IGitMacheteRepository> repositoryRef = new AtomicReference<>(null);
  private final RepositoryGraphFactory repositoryGraphFactory;
  private final VcsRootDropdown vcsRootDropdown;
  private final IGitMacheteRepositoryFactory gitMacheteRepositoryFactory;

  public GitMacheteGraphTableManager(Project project,
      ISelectionChangeObservable<GitRepository> selectionChangeObservable) {
    this.project = project;
    this.isListingCommits = false;
    GraphTableModel graphTableModel = new GraphTableModel(RepositoryGraphFactory.getNullRepositoryGraph());
    this.gitMacheteGraphTable = new GitMacheteGraphTable(graphTableModel, project, repositoryRef,
        selectionChangeObservable);
    this.repositoryGraphFactory = new RepositoryGraphFactory();
    this.vcsRootDropdown = selectionChangeObservable;
    this.gitMacheteRepositoryFactory = getGitMacheteRepositoryFactoryInstance();

    // InitializationChecker allows us to invoke instance methods below because the class is final
    // and all fields are already initialized. Hence, `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTableManager.class)`, as would be with a non-final class) at this point.
    subscribeToVcsRootChanges();
    subscribeToGitRepositoryChanges();
  }

  @SneakyThrows
  private static IGitMacheteRepositoryFactory getGitMacheteRepositoryFactoryInstance() {
    Reflections reflections = new Reflections("com.virtuslab");
    Set<Class<? extends IGitMacheteRepositoryFactory>> classes = reflections
        .getSubTypesOf(IGitMacheteRepositoryFactory.class);
    return classes.iterator().next().getDeclaredConstructor().newInstance();
  }

  private void subscribeToVcsRootChanges() {
    // The method reference is invoked when user changes repository in dropdown menu
    vcsRootDropdown.addObserver(this::updateAndRefreshInBackground);
  }

  private void subscribeToGitRepositoryChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    GitRepositoryChangeListener listener = repository -> updateAndRefreshInBackground();
    project.getMessageBus().connect().subscribe(topic, listener);
  }

  public void refreshGraphTable() {
    GitRepository gitRepository = vcsRootDropdown.getValue();
    Path macheteFilePath = getMacheteFilePath(gitRepository);
    boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

    refreshGraphTable(macheteFilePath, isMacheteFilePresent);
  }

  /** Creates a new repository graph and sets it to the graph table model. */
  private void refreshGraphTable(Path macheteFilePath, boolean isMacheteFilePresent) {
    // isUnitTestMode() checks if IDEA is running as a command line applet or in unit test mode.
    // No UI should be shown when IDEA is running in this mode.
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) {
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
      }
    }
    gitMacheteGraphTable.getModel().setRepositoryGraph(repositoryGraph);

    if (!isMacheteFilePresent) {
      gitMacheteGraphTable.setTextForEmptyGraph(
          "There is no machete file (${macheteFilePath}) for this repository.",
          "Please use 'git machete discover' CLI command to automatically create machete file.");
    }

    GuiUtils.invokeLaterIfNeeded(gitMacheteGraphTable::updateUI, ModalityState.NON_MODAL);
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
    if (project != null && !project.isDisposed()) {
      new Task.Backgroundable(project, "Updating Git Machete repository and refreshing") {
        @Override
        @UIEffect
        public void run(ProgressIndicator indicator) {
          GitRepository gitRepository = vcsRootDropdown.getValue();
          Path mainDirectoryPath = getMainDirectoryPath(gitRepository);
          Path gitDirectoryPath = getGitDirectoryPath(gitRepository);
          Path macheteFilePath = getMacheteFilePath(gitRepository);
          boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

          updateRepository(mainDirectoryPath, gitDirectoryPath, isMacheteFilePresent);
          refreshGraphTable(macheteFilePath, isMacheteFilePresent);
        }
      }.queue();
    }
  }

  /**
   * Updates repository which is the base of graph table model. The change will be seen after
   * {@link GitMacheteGraphTableManager#refreshGraphTable()}.
   */
  private void updateRepository(Path mainDirectoryPath, Path gitDirectoryPath, boolean isMacheteFilePresent) {
    if (isMacheteFilePresent) {
      var repository = Try.of(() -> gitMacheteRepositoryFactory.create(mainDirectoryPath, gitDirectoryPath))
          .onFailure(e -> LOG.error("Unable to create Git Machete repository", e)).get();
      repositoryRef.set(repository);
    } else {
      repositoryRef.set(null);
    }
  }
}
