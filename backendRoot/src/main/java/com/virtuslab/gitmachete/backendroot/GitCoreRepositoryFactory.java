package com.virtuslab.gitmachete.backendroot;

import com.virtuslab.gitcore.api.IGitCoreRepository;
import java.nio.file.Path;

public interface GitCoreRepositoryFactory {
  IGitCoreRepository create(Path pathToRoot);
}
