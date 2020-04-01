package com.virtuslab.gitcore.api;

import java.nio.file.Path;
import java.util.Optional;

import io.vavr.collection.List;

public interface IGitCoreRepository {
  Optional<IGitCoreLocalBranch> getCurrentBranch() throws GitCoreException;

  IGitCoreLocalBranch getLocalBranch(String branchName) throws GitCoreException;

  IGitCoreRemoteBranch getRemoteBranch(String branchName) throws GitCoreException;

  List<IGitCoreLocalBranch> getLocalBranches() throws GitCoreException;

  List<IGitCoreRemoteBranch> getRemoteBranches() throws GitCoreException;

  Path getRepositoryPath();

  Path getGitFolderPath();

  boolean isAncestor(BaseGitCoreCommit presumedAncestor, BaseGitCoreCommit presumedDescendant)
      throws GitCoreException;
}
