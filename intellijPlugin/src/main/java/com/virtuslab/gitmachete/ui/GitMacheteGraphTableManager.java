package com.virtuslab.gitmachete.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import com.virtuslab.gitmachete.backendroot.GitFactoryModule;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.data.RepositoryGraphFactory;
import com.virtuslab.gitmachete.ui.table.GitMacheteGraphTable;
import com.virtuslab.gitmachete.ui.table.GraphTableModel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;

public class GitMacheteGraphTableManager {
  private static final Logger LOG = Logger.getInstance(GitMacheteGraphTableManager.class);
  private final Project project;
  @Getter @Setter private boolean isListingCommits;
  @Getter private GitMacheteGraphTable gitMacheteGraphTable;
  private final GitMacheteRepositoryFactory gitMacheteRepositoryFactory;
  private IGitMacheteRepository repository;
  private final RepositoryGraphFactory repositoryGraphFactory;

  public GitMacheteGraphTableManager(@Nonnull Project project) {
    this.project = project;
    this.isListingCommits = false;
    GraphTableModel graphTableModel =
        new GraphTableModel(RepositoryGraphFactory.getNullRepositoryGraph());
    this.gitMacheteGraphTable = new GitMacheteGraphTable(graphTableModel);
    this.gitMacheteRepositoryFactory =
        GitFactoryModule.getInjector().getInstance(GitMacheteRepositoryFactory.class);
    this.repositoryGraphFactory = new RepositoryGraphFactory();
  }

  /** Creates a new repository graph and sets it to the graph table model. */
  public void refreshUI() {
    /*
     * isUnitTestMode() checks if IDEA is running as a command line applet or in unit test mode.
     * No UI should be shown when IDEA is running in this mode.
     */
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) return;

    RepositoryGraph repositoryGraph =
        repositoryGraphFactory.getRepositoryGraph(repository, isListingCommits);
    gitMacheteGraphTable.getModel().setRepositoryGraph(repositoryGraph);

    GuiUtils.invokeLaterIfNeeded(() -> gitMacheteGraphTable.updateUI(), ModalityState.NON_MODAL);
  }

  /**
   * Updates repository which is the base of graph table model. The change will be seen after {@code
   * refreshUI()}.
   */
  public void updateRepository() {
    Path pathToRepoRoot = Paths.get(Objects.requireNonNull(project.getBasePath()));
    try {
      repository = gitMacheteRepositoryFactory.create(pathToRepoRoot, /*repositoryName*/ null);
    } catch (GitMacheteException e) {
      LOG.error("Unable to create Git Machete repository", e);
    }
  }
}
