package com.virtuslab.gitcore.api;

import java.util.Optional;

import io.vavr.collection.List;

public interface IGitCoreBranch {
  String getName();

  String getFullName();

  BaseGitCoreCommit getPointedCommit() throws GitCoreException;

  boolean isLocal();

  List<BaseGitCoreCommit> computeCommitsUntil(BaseGitCoreCommit upToCommit) throws GitCoreException;

  boolean hasJustBeenCreated() throws GitCoreException;

  Optional<BaseGitCoreCommit> computeMergeBase(IGitCoreBranch branch) throws GitCoreException;
}
