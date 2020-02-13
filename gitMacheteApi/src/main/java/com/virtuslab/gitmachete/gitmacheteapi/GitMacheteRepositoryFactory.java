package com.virtuslab.gitmachete.gitmacheteapi;

import java.nio.file.Path;

public interface GitMacheteRepositoryFactory {
  IGitMacheteRepository create(Path pathToRepoRoot, String repositoryName)
      throws GitMacheteException;
}
