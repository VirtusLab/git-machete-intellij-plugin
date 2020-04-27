package com.virtuslab.gitmachete.frontend.graph.api.repository;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public interface IRepositoryGraphFactory {

  IRepositoryGraph NULL_REPOSITORY_GRAPH = new NullRepositoryGraph();

  IRepositoryGraph getRepositoryGraph(IGitMacheteRepository givenRepository, boolean isListingCommits);
}
