package com.virtuslab.gitcore.api;

import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface IGitCoreRepository {
  Option<IGitCoreLocalBranch> getCurrentBranch() throws GitCoreException;

  IGitCoreLocalBranch getLocalBranch(String branchName) throws GitCoreException;

  Option<IGitCoreRemoteBranch> getRemoteBranch(String branchName) throws GitCoreException;

  List<IGitCoreLocalBranch> getLocalBranches() throws GitCoreException;

  List<IGitCoreRemoteBranch> getRemoteBranches() throws GitCoreException;

  Path getMainDirectoryPath();

  boolean isAncestor(BaseGitCoreCommit presumedAncestor, BaseGitCoreCommit presumedDescendant)
      throws GitCoreException;
}
