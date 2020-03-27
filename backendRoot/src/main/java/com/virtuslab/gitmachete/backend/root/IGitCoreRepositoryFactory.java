package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;

import com.virtuslab.gitcore.api.IGitCoreRepository;

public interface IGitCoreRepositoryFactory {
  IGitCoreRepository create(Path pathToRoot);
}
