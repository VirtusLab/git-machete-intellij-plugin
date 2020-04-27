package com.virtuslab.gitmachete.frontend.graph.repository;

import static com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraphBuilder.DEFAULT_GET_COMMITS;
import static com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraphBuilder.EMPTY_GET_COMMITS;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public class RepositoryGraphFactory {

  @Getter
  @SuppressWarnings("ConstantName")
  public static final RepositoryGraph nullRepositoryGraph = RepositoryGraph.getNullRepositoryGraph();

  @MonotonicNonNull
  private RepositoryGraph repositoryGraphWithCommits = null;
  @MonotonicNonNull
  private RepositoryGraph repositoryGraphWithoutCommits = null;
  @MonotonicNonNull
  private IGitMacheteRepository repository = null;

  public RepositoryGraph getRepositoryGraph(IGitMacheteRepository givenRepository, boolean isListingCommits) {
    if (givenRepository != this.repository || repositoryGraphWithCommits == null
        || repositoryGraphWithoutCommits == null) {

      this.repository = givenRepository;
      RepositoryGraphBuilder repositoryGraphBuilder = new RepositoryGraphBuilder().repository(givenRepository);
      repositoryGraphWithCommits = repositoryGraphBuilder.branchGetCommitsStrategy(DEFAULT_GET_COMMITS).build();
      repositoryGraphWithoutCommits = repositoryGraphBuilder.branchGetCommitsStrategy(EMPTY_GET_COMMITS).build();
    }
    return isListingCommits ? repositoryGraphWithCommits : repositoryGraphWithoutCommits;
  }
}
