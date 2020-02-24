package com.virtuslab.gitcore.gitcoreapi;

import java.util.List;
import java.util.Optional;

public interface IGitCoreBranch {
  String getName();

  String getFullName() throws GitException;

  IGitCoreCommit getPointedCommit() throws GitException;

  boolean isLocal();

  List<IGitCoreCommit> computeCommitsUntil(IGitCoreCommit upToCommit) throws GitException;

  boolean hasJustBeenCreated() throws GitException;

  Optional<IGitCoreCommit> computeMergeBase(IGitCoreBranch branch) throws GitException;
}
