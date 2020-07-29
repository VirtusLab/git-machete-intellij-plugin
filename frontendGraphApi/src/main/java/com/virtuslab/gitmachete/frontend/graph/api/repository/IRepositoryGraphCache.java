package com.virtuslab.gitmachete.frontend.graph.api.repository;

import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;

public interface IRepositoryGraphCache {
  @UIEffect
  IRepositoryGraph getRepositoryGraph(IGitMacheteRepositorySnapshot givenRepositorySnapshot, boolean isListingCommits);
}
