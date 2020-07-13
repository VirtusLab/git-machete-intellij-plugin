package com.virtuslab.gitcore.api;

import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

public interface IGitCoreRepository {
  Option<String> deriveConfigValue(String section, String subsection, String name);

  Option<IGitCoreCommit> parseRevision(String revision) throws GitCoreException;

  List<IGitCoreLocalBranchSnapshot> deriveAllLocalBranches() throws GitCoreException;

  IGitCoreHeadSnapshot deriveHead() throws GitCoreException;

  Option<GitCoreRelativeCommitCount> deriveRelativeCommitCount(
      IGitCoreCommit fromPerspectiveOf,
      IGitCoreCommit asComparedTo) throws GitCoreException;

  List<String> deriveAllRemoteNames() throws GitCoreException;

  boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException;

  Stream<IGitCoreCommit> ancestorsOf(IGitCoreCommit commitInclusive) throws GitCoreException;

  List<IGitCoreCommit> deriveCommitRange(IGitCoreCommit fromInclusive, IGitCoreCommit untilExclusive) throws GitCoreException;
}
