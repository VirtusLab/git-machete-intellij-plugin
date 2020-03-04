package com.virtuslab.gitmachete.backendroot;

import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepositoryBuilder;
import java.nio.file.Path;

public interface GitMacheteRepositoryBuilderFactory {
  IGitMacheteRepositoryBuilder create(Path pathToRepoRoot);
}
