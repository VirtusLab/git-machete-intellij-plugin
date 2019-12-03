package com.virtuslab.gitmachete.gitmacheteapi;

import java.nio.file.Path;
import java.util.Optional;

public interface GitMacheteRepositoryFactory {
  IGitMacheteRepository create(Path pathToRepoRoot, Optional<String> repositoryName)
      throws GitMacheteException;
}
