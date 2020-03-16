package com.virtuslab.gitcore.api;

import java.util.Optional;

import io.vavr.collection.List;

public interface IGitCoreBranch {
  String getName();

  String getFullName();

  IGitCoreCommit getPointedCommit() throws GitCoreException;

  boolean isLocal();

  List<IGitCoreCommit> computeCommitsUntil(IGitCoreCommit upToCommit) throws GitCoreException;

  boolean hasJustBeenCreated() throws GitCoreException;

  Optional<IGitCoreCommit> computeMergeBase(IGitCoreBranch branch) throws GitCoreException;
}
