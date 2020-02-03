package com.virtuslab.gitmachete.graph.repositorygraph.data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.BaseRepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphWithCommits;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

public class RepositoryGraphFactory {

  @Getter
  public static final BaseRepositoryGraph nullRepositoryGraph =
      new RepositoryGraph(NullRepository.getInstance());

  @Nonnull
  public static BaseRepositoryGraph getRepositoryGraph(
      @Nullable IGitMacheteRepository repository, boolean isListingCommits) {
    if (repository == null) {
      return nullRepositoryGraph;
    } else if (isListingCommits) {
      return new RepositoryGraphWithCommits(repository);
    } else {
      return new RepositoryGraph(repository);
    }
  }
}
