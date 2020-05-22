package com.virtuslab.gitcore.api;

import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IGitCoreRepository {
  Option<IGitCoreLocalBranch> deriveCurrentBranch() throws GitCoreException;

  Option<IGitCoreLocalBranch> deriveLocalBranch(String localBranchShortName);

  List<? extends IGitCoreLocalBranch> deriveLocalBranches() throws GitCoreException;

  List<? extends IGitCoreRemoteBranch> deriveRemoteBranches(String remoteName) throws GitCoreException;

  List<? extends IGitCoreRemoteBranch> deriveAllRemoteBranches() throws GitCoreException;

  List<String> deriveAllRemotes();

  Path getMainDirectoryPath();

  boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException;
}
