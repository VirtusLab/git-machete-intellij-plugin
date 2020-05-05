package com.virtuslab.gitmachete.frontend.ui.impl.table;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;

final class GitPathUtils {

  private GitPathUtils() {}

  static Path getMainDirectoryPath(GitRepository gitRepository) {
    return Paths.get(gitRepository.getRoot().getPath());
  }

  static Path getGitDirectoryPath(GitRepository gitRepository) {
    VirtualFile vfGitDir = GitUtil.findGitDir(gitRepository.getRoot());
    assert vfGitDir != null : "Can't get .git directory from repo root path ${gitRepository.getRoot()}";
    return Paths.get(vfGitDir.getPath());
  }

  static Path getMacheteFilePath(GitRepository gitRepository) {
    return getGitDirectoryPath(gitRepository).resolve("machete");
  }

}
