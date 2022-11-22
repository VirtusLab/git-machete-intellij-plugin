package com.virtuslab.gitmachete.backend.services;

import java.nio.file.Path;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.gitmachete.backend.api.GitMacheteException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;

public final class GitMacheteRepositoryCacheService {
  private IGitMacheteRepositoryCache repositoryCache;

  public GitMacheteRepositoryCacheService() {
    repositoryCache = RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class);
  }

  public IGitMacheteRepository getInstance(Path rootDirectoryPath, Path mainGitDirectoryPath, Path worktreeGitDirectoryPath)
      throws GitMacheteException {
    return repositoryCache.getInstance(rootDirectoryPath, mainGitDirectoryPath, worktreeGitDirectoryPath);
  }
}
