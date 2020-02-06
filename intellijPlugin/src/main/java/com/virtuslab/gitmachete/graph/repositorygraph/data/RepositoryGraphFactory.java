package com.virtuslab.gitmachete.graph.repositorygraph.data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

public class RepositoryGraphFactory {

  @Getter
  public static final RepositoryGraph nullRepositoryGraph =
      new RepositoryGraph(NullRepository.getInstance(), RepositoryGraph.DEFAULT_GET_COMMITS);

  private RepositoryGraph repositoryGraphWithCommits;
  private RepositoryGraph repositoryGraphWithoutCommits;
  private IGitMacheteRepository repository;

  @Nonnull
  public RepositoryGraph getRepositoryGraphWithoutCommits(
      @Nullable IGitMacheteRepository repository, boolean isListingCommits) {
    if (repository == null) {
      return nullRepositoryGraph;
    } else {
      if (!repository.equals(this.repository)) {
        this.repository = repository;
        repositoryGraphWithCommits =
            new RepositoryGraph(repository, RepositoryGraph.DEFAULT_GET_COMMITS);
        repositoryGraphWithoutCommits =
            new RepositoryGraph(repository, RepositoryGraph.EMPTY_GET_COMMITS);
      }
      return isListingCommits ? repositoryGraphWithCommits : repositoryGraphWithoutCommits;
    }
  }
}
