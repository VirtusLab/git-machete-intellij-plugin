package com.virtuslab.gitmachete.frontend.ui.table;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.dvcs.repo.Repository;
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
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.root.BackendFactoryModule;
import com.virtuslab.gitmachete.backend.root.IGitMacheteRepositoryBuilderFactory;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraphFactory;
import com.virtuslab.gitmachete.frontend.ui.CvsRootDropdown;

public class GitMacheteGraphTableManager {
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
  private Path pathToRepoRoot;

  @SuppressWarnings("method.invocation.invalid") // for `subscribeToGitRepositoryChanges`
  public GitMacheteGraphTableManager(Project project, CvsRootDropdown cvsRootDropdown) {
    this.project = project;
    this.isListingCommits = false;
    GraphTableModel graphTableModel = new GraphTableModel(RepositoryGraphFactory.getNullRepositoryGraph());
    this.gitMacheteGraphTable = new GitMacheteGraphTable(graphTableModel, project, repositoryRef);
    this.gitMacheteRepositoryBuilderFactory = BackendFactoryModule.getInjector()
        .getInstance(IGitMacheteRepositoryBuilderFactory.class);
    this.repositoryGraphFactory = new RepositoryGraphFactory();
    this.pathToRepoRoot = Paths.get(cvsRootDropdown.getValue().getRoot().getPath());
    subscribeToCvsRootChanges(cvsRootDropdown);
    subscribeToGitRepositoryChanges();
  }

  /** Creates a new repository graph and sets it to the graph table model. */
  public void refreshUI() {
    /*
     * isUnitTestMode() checks if IDEA is running as a command line applet or in unit test mode. No UI should be shown
     * when IDEA is running in this mode.
     */
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    RepositoryGraph repositoryGraph = repositoryGraphFactory.getRepositoryGraph(repositoryRef.get(), isListingCommits);
    gitMacheteGraphTable.getModel().setRepositoryGraph(repositoryGraph);

    GuiUtils.invokeLaterIfNeeded(gitMacheteGraphTable::updateUI, ModalityState.NON_MODAL);
  }

  /**
   * Function that is invoked by {@link CvsRootDropdown#setValue()} when user change repository in dropdown menu
   */
  public void cvsRootChangeSubscriber(Repository newRepository) {
    pathToRepoRoot = Paths.get(newRepository.getRoot().getPath());
    updateAndRefreshInBackground();
  }

  private void subscribeToCvsRootChanges(CvsRootDropdown cvsRootDropdown) {
    cvsRootDropdown.subscribe(this::cvsRootChangeSubscriber);
  }

  /**
   * Updates repository which is the base of graph table model. The change will be seen after
   * {@link GitMacheteGraphTableManager#refreshUI()}.
   */
  public void updateRepository() {
    var repository = Try.of(() -> gitMacheteRepositoryBuilderFactory.create(pathToRepoRoot).build())
        .onFailure(e -> LOG.error("Unable to create Git Machete repository", e)).get();
    repositoryRef.set(repository);
  }

  public void updateAndRefreshInBackground() {
    if (project != null && !project.isDisposed()) {
      new Task.Backgroundable(project, "Updating Git Machete Repository And Refreshing") {
        @Override
        public void run(ProgressIndicator indicator) {
          updateRepository();
          refreshUI();
        }
      }.queue();
    }
  }

  private void subscribeToGitRepositoryChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    GitRepositoryChangeListener listener = repository -> updateAndRefreshInBackground();
    project.getMessageBus().connect().subscribe(topic, listener);
  }
}
