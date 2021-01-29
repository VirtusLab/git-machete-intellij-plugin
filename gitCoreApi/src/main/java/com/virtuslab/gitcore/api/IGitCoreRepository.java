package com.virtuslab.gitcore.api;

import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public interface IGitCoreRepository {
  @UIThreadUnsafe
  Option<String> deriveConfigValue(String section, String subsection, String name);

  @UIThreadUnsafe
  Option<IGitCoreCommit> parseRevision(String revision) throws GitCoreException;

  /**
   * @return snapshots of all local branches in the repository, sorted by name
   * @throws GitCoreException when reading git repository data fails
   */
  @UIThreadUnsafe
  List<IGitCoreLocalBranchSnapshot> deriveAllLocalBranches() throws GitCoreException;

  @UIThreadUnsafe
  IGitCoreHeadSnapshot deriveHead() throws GitCoreException;

  @UIThreadUnsafe
  Option<GitCoreRelativeCommitCount> deriveRelativeCommitCount(
      IGitCoreCommit fromPerspectiveOf,
      IGitCoreCommit asComparedTo) throws GitCoreException;

  @UIThreadUnsafe
  List<String> deriveAllRemoteNames();

  @UIThreadUnsafe
  boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException;

  @UIThreadUnsafe
  Stream<IGitCoreCommit> ancestorsOf(IGitCoreCommit commitInclusive) throws GitCoreException;

  @UIThreadUnsafe
  List<IGitCoreCommit> deriveCommitRange(IGitCoreCommit fromInclusive, IGitCoreCommit untilExclusive) throws GitCoreException;

  @UIThreadUnsafe
  GitCoreRepositoryState deriveRepositoryState();
}
