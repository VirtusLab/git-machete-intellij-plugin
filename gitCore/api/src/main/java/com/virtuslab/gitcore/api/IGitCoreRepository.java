package com.virtuslab.gitcore.api;

import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public interface IGitCoreRepository {

  Path getRootDirectoryPath();
  Path getMainGitDirectoryPath();
  Path getWorktreeGitDirectoryPath();

  @UIThreadUnsafe
  @Nullable
  String deriveConfigValue(String section, String subsection, String name);

  @UIThreadUnsafe
  @Nullable
  String deriveConfigValue(String section, String name);

  @UIThreadUnsafe
  @Nullable
  IGitCoreCommit parseRevision(String revision) throws GitCoreException;

  /**
   * @return snapshots of all local branches in the repository, sorted by name
   * @throws GitCoreException when reading git repository data fails
   */
  @UIThreadUnsafe
  List<IGitCoreLocalBranchSnapshot> deriveAllLocalBranches() throws GitCoreException;

  @UIThreadUnsafe
  IGitCoreHeadSnapshot deriveHead() throws GitCoreException;

  @UIThreadUnsafe
  @Nullable
  GitCoreRelativeCommitCount deriveRelativeCommitCount(
      IGitCoreCommit fromPerspectiveOf,
      IGitCoreCommit asComparedTo) throws GitCoreException;

  @UIThreadUnsafe
  List<String> deriveAllRemoteNames();

  @UIThreadUnsafe
  @Nullable
  String deriveRebasedBranch() throws GitCoreException;

  @UIThreadUnsafe
  @Nullable
  String deriveBisectedBranch() throws GitCoreException;

  @UIThreadUnsafe
  boolean isAncestorOrEqual(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException;

  @UIThreadUnsafe
  Stream<IGitCoreCommit> ancestorsOf(IGitCoreCommit commitInclusive) throws GitCoreException;

  @UIThreadUnsafe
  List<IGitCoreCommit> deriveCommitRange(IGitCoreCommit fromInclusive, IGitCoreCommit untilExclusive) throws GitCoreException;

  @UIThreadUnsafe
  GitCoreRepositoryState deriveRepositoryState();
}
