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
import com.virtuslab.gitmachete.graph.repositorygraph.BaseRepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.data.RepositoryGraphFactory;
import com.virtuslab.gitmachete.ui.table.GitMacheteGraphTable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
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

  public GitMacheteGraphTableManager(@Nonnull Project project) {
    this.project = project;
    this.isListingCommits = false;
    this.gitMacheteGraphTable =
        new GitMacheteGraphTable(RepositoryGraphFactory.getNullRepositoryGraph());
    this.gitMacheteRepositoryFactory =
        GitFactoryModule.getInjector().getInstance(GitMacheteRepositoryFactory.class);
  }

  public void refreshUI(boolean useCache) {
    Timer.start("refreshUI");
    /*
     * isUnitTestMode() checks if IDEA is running as a command line applet or in unit test mode.
     * No UI should be shown when IDEA is running in this mode.
     */
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) return;

    BaseRepositoryGraph repositoryGraph =
        RepositoryGraphFactory.getRepositoryGraph(repository, isListingCommits, useCache);
    gitMacheteGraphTable.getGraphTableModel().setRepositoryGraph(repositoryGraph);

    GuiUtils.invokeLaterIfNeeded(
        () -> {
          Timer.start("refreshUI (invoke later)");
          gitMacheteGraphTable.updateUI();
          Timer.check("refreshUI (invoke later)");
        },
        ModalityState.NON_MODAL);
    Timer.check("refreshUI");
  }

  public void updateModelGraphRepository() {
    Timer.start("modelUpdate");
    Path pathToRepoRoot = Paths.get(Objects.requireNonNull(project.getBasePath()));
    try {
      repository = gitMacheteRepositoryFactory.create(pathToRepoRoot, Optional.empty());
    } catch (GitMacheteException e) {
      LOG.error("Unable to create Git Machete repository", e);
    }
    Timer.check("modelUpdate");
  }
}
