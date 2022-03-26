package com.virtuslab.gitcore.api;

import java.nio.file.Path;

public interface IGitCoreRepositoryFactory {
  IGitCoreRepository create(Path rootDirectoryPath, Path mainGitDirectoryPath, Path worktreeGitDirectoryPath)
      throws GitCoreException;
}
