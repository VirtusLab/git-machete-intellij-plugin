package com.virtuslab.gitcore.api;

import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IGitCoreRepository {
  Option<IGitCoreLocalBranch> getCurrentBranch() throws GitCoreException;

  IGitCoreLocalBranch getLocalBranch(String branchName) throws GitCoreException;

  Option<IGitCoreRemoteBranch> getRemoteBranch(String branchName, String remoteName) throws GitCoreException;

  List<IGitCoreLocalBranch> getLocalBranches() throws GitCoreException;

  List<IGitCoreRemoteBranch> getRemoteBranches(String remoteName) throws GitCoreException;

  List<IGitCoreRemoteBranch> getAllRemoteBranches() throws GitCoreException;

  List<String> getRemotes();

  Path getMainDirectoryPath();

  boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException;
}
