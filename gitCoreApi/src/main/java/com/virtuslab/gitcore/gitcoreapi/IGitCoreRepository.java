package com.virtuslab.gitcore.gitcoreapi;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public interface IGitCoreRepository {
  Optional<IGitCoreLocalBranch> getCurrentBranch() throws GitException;

  IGitCoreLocalBranch getLocalBranch(String branchName) throws GitException;

  IGitCoreRemoteBranch getRemoteBranch(String branchName) throws GitException;

  Path getRepositoryPath();

  Path getGitFolderPath();

  Map<String, IGitCoreSubmoduleEntry> getSubmodules() throws GitException;
}
