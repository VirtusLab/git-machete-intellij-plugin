package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;

public interface GitMacheteRepositoryBuilderFactory {
  IGitMacheteRepositoryBuilder create(Path pathToRepoRoot);
}
