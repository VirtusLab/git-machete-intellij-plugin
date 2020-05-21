package com.virtuslab.gitcore.api;

import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IGitCoreRepository {
  Option<? extends IGitCoreLocalBranch> getCurrentBranch() throws GitCoreException;

  IGitCoreLocalBranch getLocalBranch(String branchName) throws GitCoreException;

  Option<? extends IGitCoreRemoteBranch> getRemoteBranch(String branchName, String remoteName) throws GitCoreException;

  List<? extends IGitCoreLocalBranch> getLocalBranches() throws GitCoreException;

  List<? extends IGitCoreRemoteBranch> getRemoteBranches(String remoteName) throws GitCoreException;

  List<? extends IGitCoreRemoteBranch> getAllRemoteBranches() throws GitCoreException;

  List<String> getRemotes();

  Path getMainDirectoryPath();

  boolean isAncestor(IGitCoreCommit presumedAncestor, IGitCoreCommit presumedDescendant) throws GitCoreException;
}
