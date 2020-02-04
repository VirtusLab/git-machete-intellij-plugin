package com.virtuslab.gitmachete.graph.repositorygraph.data;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.graph.repositorygraph.BaseRepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraph;
import com.virtuslab.gitmachete.graph.repositorygraph.RepositoryGraphWithCommits;
import com.virtuslab.gitmachete.ui.Timer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

public class RepositoryGraphFactory {

  @Getter
  public static final BaseRepositoryGraph nullRepositoryGraph =
      new RepositoryGraph(NullRepository.getInstance());

  private static RepositoryGraphWithCommits repositoryGraphWithCommits;
  private static RepositoryGraph repositoryGraph;

  @Nonnull
  public static BaseRepositoryGraph getRepositoryGraph(
      @Nullable IGitMacheteRepository repository, boolean isListingCommits, boolean useCache) {
    if (repository == null) {
      return nullRepositoryGraph;
    } else if (isListingCommits) {
      if (repositoryGraphWithCommits == null || !useCache) {
        Timer.start("createRGWC");
        repositoryGraphWithCommits = new RepositoryGraphWithCommits(repository);
        Timer.check("createRGWC");
      }
      return repositoryGraphWithCommits;
    } else {
      if (repositoryGraph == null || !useCache) {
        Timer.start("createRG");
        repositoryGraph = new RepositoryGraph(repository);
        Timer.check("createRG");
      }
      return repositoryGraph;
    }
  }
}
