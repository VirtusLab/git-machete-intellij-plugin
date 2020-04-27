package com.virtuslab.gitmachete.frontend.graph.impl.repository;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphFactory;

public class RepositoryGraphFactory implements IRepositoryGraphFactory {

  @MonotonicNonNull
  private IRepositoryGraph repositoryGraphWithCommits = null;
  @MonotonicNonNull
  private IRepositoryGraph repositoryGraphWithoutCommits = null;
  @MonotonicNonNull
  private IGitMacheteRepository repository = null;

  public IRepositoryGraph getRepositoryGraph(IGitMacheteRepository givenRepository, boolean isListingCommits) {
    if (givenRepository != this.repository || repositoryGraphWithCommits == null
        || repositoryGraphWithoutCommits == null) {

      this.repository = givenRepository;
      RepositoryGraphBuilder repositoryGraphBuilder = new RepositoryGraphBuilder().repository(givenRepository);
      repositoryGraphWithCommits = repositoryGraphBuilder
          .branchGetCommitsStrategy(RepositoryGraphBuilder.DEFAULT_GET_COMMITS).build();
      repositoryGraphWithoutCommits = repositoryGraphBuilder
          .branchGetCommitsStrategy(RepositoryGraphBuilder.EMPTY_GET_COMMITS).build();
    }
    return isListingCommits ? repositoryGraphWithCommits : repositoryGraphWithoutCommits;
  }
}
