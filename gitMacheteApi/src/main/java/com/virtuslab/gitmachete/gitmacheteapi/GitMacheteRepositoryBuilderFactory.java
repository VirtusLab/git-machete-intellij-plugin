package com.virtuslab.gitmachete.gitmacheteapi;

import java.nio.file.Path;

public interface GitMacheteRepositoryBuilderFactory {
  IGitMacheteRepositoryBuilder create(Path pathToRepoRoot);
}
