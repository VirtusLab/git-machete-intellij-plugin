package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;

public interface IGitMacheteRepositoryBuilderFactory {
  IGitMacheteRepositoryBuilder create(Path pathToRepoRoot);
}
