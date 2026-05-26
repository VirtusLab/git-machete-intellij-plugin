package com.virtuslab.gitmachete.backend.api;

import java.nio.file.Path;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

/** Each implementing class must have a public parameterless constructor. */
public interface IGitMacheteRepositoryCache {
  @FunctionalInterface
  interface Injector {
    <T> T inject(Class<T> clazz);
  }

  @UIThreadUnsafe
  IGitMacheteRepository getInstance(Path rootDirectoryPath, Injector injector) throws GitMacheteException;
}
