package com.virtuslab.gitmachete.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.ui.GuiUtils;
import com.virtuslab.gitmachete.backendroot.GitFactoryModule;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteRepositoryFactory;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.IRepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.data.RepositoryGraphFactory;
import com.virtuslab.gitmachete.ui.table.GitMacheteGraphTable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

public class GitMacheteGraphTableManager {
  private final Project project;
  @Getter @Setter private boolean isListingCommits;
  @Getter private GitMacheteGraphTable gitMacheteGraphTable;
  private final GitMacheteRepositoryFactory gitMacheteRepositoryFactory;

  public GitMacheteGraphTableManager(@Nonnull Project project) {
    this.project = project;
    this.isListingCommits = false;
    this.gitMacheteGraphTable =
        new GitMacheteGraphTable(RepositoryGraphFactory.getNullRepositoryGraph());
    this.gitMacheteRepositoryFactory =
        GitFactoryModule.getInjector().getInstance(GitMacheteRepositoryFactory.class);
  }

  public void updateModel() {
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) return;
    if (!ProjectLevelVcsManager.getInstance(project).hasActiveVcss()) return;

    IGitMacheteRepository repository = createRepository(project);
    IRepositoryGraph repositoryGraph =
        RepositoryGraphFactory.getRepositoryGraph(repository, isListingCommits);

    gitMacheteGraphTable.getGraphTableModel().setIRepositoryGraph(repositoryGraph);
    GuiUtils.invokeLaterIfNeeded(() -> gitMacheteGraphTable.updateUI(), ModalityState.NON_MODAL);
  }

  @Nullable
  private IGitMacheteRepository createRepository(@Nonnull Project project) {
    Path pathToRepoRoot = Paths.get(Objects.requireNonNull(project.getBasePath()));
    IGitMacheteRepository repository = null;
    try {
      repository = gitMacheteRepositoryFactory.create(pathToRepoRoot, Optional.empty());
    } catch (GitMacheteException e) {
      e.printStackTrace();
    }
    return repository;
  }
}
