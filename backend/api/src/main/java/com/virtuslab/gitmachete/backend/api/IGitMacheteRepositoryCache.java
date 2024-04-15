package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

/** Each implementing class must have a public parameterless constructor. */
public interface IGitMacheteRepositoryCache {
  @FunctionalInterface
  public interface Injector {
    <T> T inject(Class<T> clazz);
  }

  @UIThreadUnsafe
  IGitMacheteRepository getInstance(Path rootDirectoryPath, Path mainGitDirectoryPath, Path worktreeGitDirectoryPath,
      Injector injector)
      throws GitMacheteException;
}
