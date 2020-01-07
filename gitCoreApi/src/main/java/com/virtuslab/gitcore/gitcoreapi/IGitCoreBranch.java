package com.virtuslab.gitcore.gitcoreapi;

import java.util.List;

public interface IGitCoreBranch {
  String getName() throws GitException;

  String getFullName() throws GitException;

  IGitCoreCommit getPointedCommit() throws GitException;

  boolean isLocal();

  List<IGitCoreCommit> getCommitsUntil(IGitCoreCommit upToCommit) throws GitException;

  boolean hasJustBeenCreated() throws GitException;
}
