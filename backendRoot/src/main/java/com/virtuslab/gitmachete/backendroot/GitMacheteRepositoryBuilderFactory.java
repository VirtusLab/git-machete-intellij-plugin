package com.virtuslab.gitmachete.backendroot;

import java.nio.file.Path;

public interface GitMacheteRepositoryBuilderFactory {
  IGitMacheteRepositoryBuilder create(Path pathToRepoRoot);
}
