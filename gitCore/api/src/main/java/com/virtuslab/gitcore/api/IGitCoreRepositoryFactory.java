package com.virtuslab.gitcore.api;

import java.nio.file.Path;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public interface IGitCoreRepositoryFactory {
  @UIThreadUnsafe
  IGitCoreRepository create(Path rootDirectoryPath) throws GitCoreException;
}
