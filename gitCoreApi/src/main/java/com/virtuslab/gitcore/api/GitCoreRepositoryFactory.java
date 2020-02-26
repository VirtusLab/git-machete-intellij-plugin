package com.virtuslab.gitcore.api;

import java.nio.file.Path;

public interface GitCoreRepositoryFactory {
  IGitCoreRepository create(Path pathToRoot);
}
