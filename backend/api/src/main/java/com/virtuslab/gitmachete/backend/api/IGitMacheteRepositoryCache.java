package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

/** Each implementing class must have a public parameterless constructor. */
public interface IGitMacheteRepositoryCache {
  IGitMacheteRepository getInstance(Path rootDirectoryPath, Path mainGitDirectoryPath, Path worktreeGitDirectoryPath)
      throws GitMacheteException;
}
