package com.virtuslab.gitmachete.backend.root;

import java.io.IOException;
import java.nio.file.Path;

import com.virtuslab.gitcore.api.GitCoreNoSuchRepositoryException;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteJGitException;

public class Utils {
  private Utils() {}

  public static Path getGitDirectoryPathByRepoRootPath(Path repoRootPath) throws GitMacheteJGitException {
    try {
      return GitCoreRepository.getGitFolderPathByRepoRootPath(repoRootPath);
    } catch (IOException | GitCoreNoSuchRepositoryException e) {
      throw new GitMacheteJGitException(e);
    }
  }
}
