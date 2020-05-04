package com.virtuslab.gitmachete.frontend.graph.api.repository;

import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public interface IRepositoryGraphFactory {

  IRepositoryGraph NULL_REPOSITORY_GRAPH = new NullRepositoryGraph();

  @UIEffect
  IRepositoryGraph getRepositoryGraph(IGitMacheteRepository givenRepository, boolean isListingCommits);
}
