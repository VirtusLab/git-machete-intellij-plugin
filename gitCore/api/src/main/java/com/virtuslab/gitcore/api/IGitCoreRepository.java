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

  /** <i>Any</i> merge base, as in, in the rare case of criss-cross histories there might be <b>multiple merge bases</b>.
   * Still, Git Machete isn't well suited to handling such cases, as it generally endorses and deals with linear histories,
   * which use merge commits rarely, and hence are very unlikely to introduce criss-cross histories.
   */
  @UIThreadUnsafe
  @Nullable
  IGitCoreCommit deriveAnyMergeBase(IGitCoreCommit commit1, IGitCoreCommit commit2) throws GitCoreException;

  @UIThreadUnsafe
  Stream<IGitCoreCommit> ancestorsOf(IGitCoreCommit commitInclusive) throws GitCoreException;

  @UIThreadUnsafe
  List<IGitCoreCommit> deriveCommitRange(IGitCoreCommit fromInclusive, IGitCoreCommit untilExclusive) throws GitCoreException;

  @UIThreadUnsafe
  GitCoreRepositoryState deriveRepositoryState();
}
