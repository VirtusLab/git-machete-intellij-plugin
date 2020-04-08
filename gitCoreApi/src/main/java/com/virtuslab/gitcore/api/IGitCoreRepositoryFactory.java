package com.virtuslab.gitcore.api;

import java.nio.file.Path;

public interface IGitCoreRepositoryFactory {
  IGitCoreRepository create(Path pathToRoot) throws GitCoreException;
}
