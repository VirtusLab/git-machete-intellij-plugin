package com.virtuslab.gitmachete.backend.api;

import java.util.Optional;

import io.vavr.collection.List;

public interface IGitMacheteBranch {
  String getName();

  List<IGitMacheteCommit> getCommits();

  IGitMacheteCommit getPointedCommit();

  Optional<IGitMacheteBranch> getUpstreamBranch();

  List<IGitMacheteBranch> getDownstreamBranches();

  Optional<String> getCustomAnnotation();

  SyncToParentStatus getSyncToParentStatus();

  SyncToOriginStatus getSyncToOriginStatus();

  Optional<IGitMacheteCommit> deriveForkPoint() throws GitMacheteException;
}
