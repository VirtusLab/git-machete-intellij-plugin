package com.virtuslab.gitmachete.backend.root;

import java.nio.file.Path;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.impl.jgit.GitCoreRepository;
import com.virtuslab.gitmachete.backend.api.GitMacheteJGitException;

public final class GitUtils {
  private GitUtils() {}

  public static Path getGitDirectoryPathByRepoRootPath(Path repoRootPath) throws GitMacheteJGitException {
    try {
      return GitCoreRepository.getGitDirectoryPathByRepoRootPath(repoRootPath);
    } catch (GitCoreException e) {
      throw new GitMacheteJGitException(e);
    }
  }
}
