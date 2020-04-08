package com.virtuslab.gitmachete.frontend.ui.table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import com.intellij.util.messages.Topic;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.root.BackendFactoryModule;
import com.virtuslab.gitmachete.backend.root.GitUtils;
import com.virtuslab.gitmachete.backend.root.IGitMacheteRepositoryBuilderFactory;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraphFactory;
import com.virtuslab.gitmachete.frontend.ui.VcsRootDropdown;

public final class GitMacheteGraphTableManager {
  private static final Logger LOG = Logger.getInstance(GitMacheteGraphTableManager.class);
  private final Project project;
  @Getter
  @Setter
  private boolean isListingCommits;
  @Getter
  private final GitMacheteGraphTable gitMacheteGraphTable;
  private final IGitMacheteRepositoryBuilderFactory gitMacheteRepositoryBuilderFactory;
  private final AtomicReference<@Nullable IGitMacheteRepository> repositoryRef = new AtomicReference<>();
  private final RepositoryGraphFactory repositoryGraphFactory;
  private final VcsRootDropdown vcsRootDropdown;

  public GitMacheteGraphTableManager(Project project, VcsRootDropdown vcsRootDropdown) {
    this.project = project;
    this.isListingCommits = false;
    GraphTableModel graphTableModel = new GraphTableModel(RepositoryGraphFactory.getNullRepositoryGraph());
    this.gitMacheteGraphTable = new GitMacheteGraphTable(graphTableModel, project, repositoryRef, vcsRootDropdown);
    this.gitMacheteRepositoryBuilderFactory = BackendFactoryModule.getInjector()
        .getInstance(IGitMacheteRepositoryBuilderFactory.class);
    this.repositoryGraphFactory = new RepositoryGraphFactory();
    this.vcsRootDropdown = vcsRootDropdown;

    // InitalizationChecker allows us to invoke instance methods below because the class is final
    // and all fields are already initialized. Hence, `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTableManager.class)`, as would be with a non-final class) at this point.
    subscribeToVcsRootChanges();
    subscribeToGitRepositoryChanges();
  }

  public void refreshGraphTable() {
    Path repoRootPath = Paths.get(vcsRootDropdown.getValue().getRoot().getPath());
    Path macheteFilePath = getMacheteFilePath(repoRootPath);
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

    // TODO (#176): When machete file is not present or it's empty propose using automatically detected (by discover
    // functionality) branch layout

    IGitMacheteRepository gitMacheteRepository = repositoryRef.get();
    RepositoryGraph repositoryGraph;
    if (gitMacheteRepository == null) {
      repositoryGraph = RepositoryGraph.getNullRepositoryGraph();
    } else {
      repositoryGraph = repositoryGraphFactory.getRepositoryGraph(gitMacheteRepository, isListingCommits);
      if (gitMacheteRepository.getRootBranches().isEmpty()) {
        gitMacheteGraphTable.setTextForEmptyGraph(
            "Your machete file is empty. Please use \"git machete discover\" CLI command to automatically fill in the machete file.");
      }
    }
    gitMacheteGraphTable.getModel().setRepositoryGraph(repositoryGraph);

    if (!isMacheteFilePresent) {
      gitMacheteGraphTable.setTextForEmptyGraph(
          String.format(
              "There is no machete file (%s) for this repository. Please use \"git machete discover\" CLI command to automatically create machete file.",
              macheteFilePath.toString()));
    }

    GuiUtils.invokeLaterIfNeeded(gitMacheteGraphTable::updateUI, ModalityState.NON_MODAL);
  }

  /**
   * Lambda inside ths function is invoked by {@link VcsRootDropdown#setValue()} when user changes repository in dropdown menu
   */
  private void subscribeToVcsRootChanges() {
    vcsRootDropdown.subscribe(this::updateAndRefreshInBackground);
  }

  private Path getMacheteFilePath(Path repoRootPath) {
    return Try.of(() -> GitUtils.getGitDirectoryPathByRepoRootPath(repoRootPath).resolve("machete"))
        .onFailure(e -> LOG.error("Unable to get machete file path", e))
        .get();
  }

  public void updateAndRefreshInBackground() {
    if (project != null && !project.isDisposed()) {
      new Task.Backgroundable(project, "Updating Git Machete Repository And Refreshing") {
        @Override
        @UIEffect
        public void run(ProgressIndicator indicator) {
          Path repoRootPath = Paths.get(vcsRootDropdown.getValue().getRoot().getPath());
          Path macheteFilePath = getMacheteFilePath(repoRootPath);
          boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);
          updateRepository(repoRootPath, isMacheteFilePresent);
          refreshGraphTable(macheteFilePath, isMacheteFilePresent);
        }
      }.queue();
    }
  }

  /**
   * Updates repository which is the base of graph table model. The change will be seen after
   * {@link GitMacheteGraphTableManager#refreshGraphTable()}.
   */
  public void updateRepository(Path repoRootPath, boolean isMacheteFilePresent) {
    if (isMacheteFilePresent) {
      var repository = Try.of(() -> gitMacheteRepositoryBuilderFactory.create(repoRootPath).build())
          .onFailure(e -> LOG.error("Unable to create Git Machete repository", e)).get();
      repositoryRef.set(repository);
    } else {
      repositoryRef.set(null);
    }
  }

  private void subscribeToGitRepositoryChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    GitRepositoryChangeListener listener = repository -> updateAndRefreshInBackground();
    project.getMessageBus().connect().subscribe(topic, listener);
  }
}
