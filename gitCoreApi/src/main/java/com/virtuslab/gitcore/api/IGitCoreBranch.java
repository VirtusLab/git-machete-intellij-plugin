package com.virtuslab.gitcore.api;

import io.vavr.collection.List;

/** All implementations should extend {@link com.virtuslab.gitcore.api.BaseGitCoreBranch} */
public interface IGitCoreBranch {
  String getName();

  String getFullName();

  BaseGitCoreCommit getPointedCommit() throws GitCoreException;

  boolean isLocal();

  List<BaseGitCoreCommit> deriveCommitsUntil(BaseGitCoreCommit upToCommit) throws GitCoreException;

  boolean hasJustBeenCreated() throws GitCoreException;
}
