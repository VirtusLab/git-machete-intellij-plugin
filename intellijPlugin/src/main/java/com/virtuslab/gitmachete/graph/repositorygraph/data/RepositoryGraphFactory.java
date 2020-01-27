package com.virtuslab.gitmachete.graph.repositorygraph.data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphImpl;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphWithCommitsImpl;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

public class RepositoryGraphFactory {

  @Getter
  public static final RepositoryGraph nullRepositoryGraph =
      new RepositoryGraphImpl(NullRepository.getInstance());

  @Nonnull
  public static RepositoryGraph getRepositoryGraph(
      @Nullable IGitMacheteRepository repository, boolean isListingCommits) {
    if (repository == null) {
      return nullRepositoryGraph;
    } else if (isListingCommits) {
      return new RepositoryGraphWithCommitsImpl(repository);
    } else {
      return new RepositoryGraphImpl(repository);
    }
  }
}
