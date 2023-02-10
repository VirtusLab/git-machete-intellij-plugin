package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

/** Each implementing class must have a public parameterless constructor. */
public interface IGitMacheteRepositoryCache {
  @UIThreadUnsafe
  IGitMacheteRepository getInstance(Path rootDirectoryPath, Path mainGitDirectoryPath, Path worktreeGitDirectoryPath)
      throws GitMacheteException;
}
