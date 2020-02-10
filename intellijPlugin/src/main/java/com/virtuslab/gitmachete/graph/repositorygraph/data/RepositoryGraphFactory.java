package com.virtuslab.gitmachete.graph.repositorygraph.data;

import static com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphBuilder.DEFAULT_GET_COMMITS;
import static com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphBuilder.EMPTY_GET_COMMITS;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphBuilder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

public class RepositoryGraphFactory {

  @Getter
  public static final RepositoryGraph nullRepositoryGraph =
      RepositoryGraph.getNullRepositoryGraph();

  private RepositoryGraph repositoryGraphWithCommits;
  private RepositoryGraph repositoryGraphWithoutCommits;
  private IGitMacheteRepository repository;

  @Nonnull
  public RepositoryGraph getRepositoryGraph(
      @Nullable IGitMacheteRepository repository, boolean isListingCommits) {
    if (repository == null) {
      return nullRepositoryGraph;
    } else {
      if (repository != this.repository) {
        this.repository = repository;

        // todo use vavr lazy (?)
        RepositoryGraphBuilder repositoryGraphBuilder =
            new RepositoryGraphBuilder().repository(repository);
        repositoryGraphWithCommits =
            repositoryGraphBuilder.branchGetCommitsStrategy(DEFAULT_GET_COMMITS).build();
        repositoryGraphWithoutCommits =
            repositoryGraphBuilder.branchGetCommitsStrategy(EMPTY_GET_COMMITS).build();
        ;
      }
      return isListingCommits ? repositoryGraphWithCommits : repositoryGraphWithoutCommits;
    }
  }
}
