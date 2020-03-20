package com.virtuslab.gitmachete.frontend.graph.repositorygraph;

import static com.virtuslab.gitmachete.frontend.graph.repositorygraph.RepositoryGraphBuilder.DEFAULT_GET_COMMITS;
import static com.virtuslab.gitmachete.frontend.graph.repositorygraph.RepositoryGraphBuilder.EMPTY_GET_COMMITS;

import javax.annotation.Nullable;

import lombok.Getter;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public class RepositoryGraphFactory {

  @Getter
  public static final RepositoryGraph nullRepositoryGraph = RepositoryGraph.getNullRepositoryGraph();

  private RepositoryGraph repositoryGraphWithCommits;
  private RepositoryGraph repositoryGraphWithoutCommits;
  private IGitMacheteRepository repository;

  public RepositoryGraph getRepositoryGraph(@Nullable IGitMacheteRepository repository, boolean isListingCommits) {
    if (repository == null) {
      return nullRepositoryGraph;
    } else {
      if (repository != this.repository) {
        this.repository = repository;

        RepositoryGraphBuilder repositoryGraphBuilder = new RepositoryGraphBuilder().repository(repository);
        repositoryGraphWithCommits = repositoryGraphBuilder.branchComputeCommitsStrategy(DEFAULT_GET_COMMITS).build();
        repositoryGraphWithoutCommits = repositoryGraphBuilder.branchComputeCommitsStrategy(EMPTY_GET_COMMITS).build();
      }
      return isListingCommits ? repositoryGraphWithCommits : repositoryGraphWithoutCommits;
    }
  }
}
