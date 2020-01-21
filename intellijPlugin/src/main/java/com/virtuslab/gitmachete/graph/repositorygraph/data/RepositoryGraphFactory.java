package com.virtuslab.gitmachete.graph.repositorygraph.data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.IRepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphImpl;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphWithCommitsImpl;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

public class RepositoryGraphFactory {

  @Getter
  public static final IRepositoryGraph nullRepositoryGraph =
      new RepositoryGraphImpl(NullRepository.getInstance());

  @Nonnull
  public static IRepositoryGraph getRepositoryGraph(
      @Nullable IGitMacheteRepository repository, boolean listCommits) {
    if (repository == null) {
      return nullRepositoryGraph;
    } else if (listCommits) {
      return new RepositoryGraphWithCommitsImpl(repository);
    } else {
      return new RepositoryGraphImpl(repository);
    }
  }
}
