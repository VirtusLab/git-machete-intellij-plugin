package com.virtuslab.gitmachete.frontend.graph.impl.repository;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.api.repository.IRepositoryGraphCache;

public class RepositoryGraphCache implements IRepositoryGraphCache {

  private @MonotonicNonNull IRepositoryGraph repositoryGraphWithCommits = null;
  private @MonotonicNonNull IRepositoryGraph repositoryGraphWithoutCommits = null;
  private @MonotonicNonNull IGitMacheteRepositorySnapshot repositorySnapshot = null;

  @Override
  @SuppressWarnings("regexp") // to allow for `synchronized`
  public synchronized IRepositoryGraph getRepositoryGraph(
      @FindDistinct IGitMacheteRepositorySnapshot givenRepositorySnapshot,
      boolean isListingCommits) {

    if (givenRepositorySnapshot != this.repositorySnapshot || repositoryGraphWithCommits == null
        || repositoryGraphWithoutCommits == null) {

      this.repositorySnapshot = givenRepositorySnapshot;
      RepositoryGraphBuilder repositoryGraphBuilder = new RepositoryGraphBuilder().repositorySnapshot(givenRepositorySnapshot);
      repositoryGraphWithCommits = repositoryGraphBuilder
          .branchGetCommitsStrategy(RepositoryGraphBuilder.DEFAULT_GET_COMMITS).build();
      repositoryGraphWithoutCommits = repositoryGraphBuilder
          .branchGetCommitsStrategy(RepositoryGraphBuilder.EMPTY_GET_COMMITS).build();
    }
    return isListingCommits ? repositoryGraphWithCommits : repositoryGraphWithoutCommits;
  }
}
