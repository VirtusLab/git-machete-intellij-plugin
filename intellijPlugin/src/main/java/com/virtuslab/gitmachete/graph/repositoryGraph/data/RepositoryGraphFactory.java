package com.virtuslab.gitmachete.graph.repositoryGraph.data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositoryGraph.IRepositoryGraph;
import com.virtuslab.gitmachete.graph.repositoryGraph.RepositoryGraphImpl;
import com.virtuslab.gitmachete.graph.repositoryGraph.RepositoryGraphWithCommits;

public class RepositoryGraphFactory {

  private static final IRepositoryGraph emptyRepositoryGraph =
      new RepositoryGraphImpl(EmptyRepository.getInstance());

  public static IRepositoryGraph getRepositoryGraph(
      IGitMacheteRepository repository, boolean listCommits) {
    if (repository == null) {
      return emptyRepositoryGraph;
    } else if (listCommits) {
      return new RepositoryGraphWithCommits(repository);
    } else {
      return new RepositoryGraphImpl(repository);
    }
  }
}
