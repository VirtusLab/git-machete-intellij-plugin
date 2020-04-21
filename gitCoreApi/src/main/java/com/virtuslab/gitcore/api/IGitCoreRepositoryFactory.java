package com.virtuslab.gitcore.api;

import java.nio.file.Path;

public interface IGitCoreRepositoryFactory {
  IGitCoreRepository create(Path mainDirectoryPath, Path gitDirectoryPath) throws GitCoreException;
}
