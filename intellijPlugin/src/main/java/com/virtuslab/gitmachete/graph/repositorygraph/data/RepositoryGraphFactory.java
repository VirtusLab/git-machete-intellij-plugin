package com.virtuslab.gitmachete.graph.repositorygraph.data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.IRepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphImpl;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphWithCommitsImpl;

public class RepositoryGraphFactory {

  private static final IRepositoryGraph emptyRepositoryGraph =
      new RepositoryGraphImpl(NullRepository.getInstance());

  public static IRepositoryGraph getRepositoryGraph(
      IGitMacheteRepository repository, boolean listCommits) {
    if (repository == null) {
      return emptyRepositoryGraph;
    } else if (listCommits) {
      return new RepositoryGraphWithCommitsImpl(repository);
    } else {
      return new RepositoryGraphImpl(repository);
    }
  }
}
