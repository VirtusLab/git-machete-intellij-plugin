package com.virtuslab.gitcore.api;

import io.vavr.collection.List;

public interface IGitCoreBranch {
  String getName();

  String getFullName();

  IGitCoreCommit getPointedCommit() throws GitCoreException;

  boolean isLocal();

  List<IGitCoreCommit> deriveCommitsUntil(IGitCoreCommit upToCommit) throws GitCoreException;

  boolean hasJustBeenCreated() throws GitCoreException;
}
