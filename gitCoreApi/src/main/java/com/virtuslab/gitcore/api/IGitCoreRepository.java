package com.virtuslab.gitcore.api;

import java.util.function.Predicate;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IGitCoreRepository {
  List<IGitCoreLocalBranch> deriveAllLocalBranches() throws GitCoreException;

  Option<IGitCoreLocalBranch> deriveCurrentBranch() throws GitCoreException;

  Option<IGitCoreLocalBranch> deriveLocalBranchByShortName(String localBranchShortName);

  Option<GitCoreBranchTrackingStatus> deriveRemoteTrackingStatus(IGitCoreLocalBranch localBranch) throws GitCoreException;

  List<IGitCoreRemoteBranch> deriveAllRemoteBranches() throws GitCoreException;

  boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException;

  Option<IGitCoreCommit> findFirstAncestor(
      IGitCoreCommit fromInclusive,
      Predicate<IGitCoreCommitHash> predicate) throws GitCoreException;

  List<IGitCoreCommit> deriveCommitRange(IGitCoreCommit fromInclusive, IGitCoreCommit untilExclusive) throws GitCoreException;
}
