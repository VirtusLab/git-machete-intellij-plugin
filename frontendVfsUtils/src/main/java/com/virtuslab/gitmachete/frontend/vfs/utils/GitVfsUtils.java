package com.virtuslab.gitmachete.frontend.vfs.utils;

import java.nio.file.Path;

import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;

public final class GitVfsUtils {

  private GitVfsUtils() {}

  public static VirtualFile getMainDirectory(GitRepository gitRepository) {
    return gitRepository.getRoot();
  }

  public static VirtualFile getGitDirectory(GitRepository gitRepository) {
    VirtualFile vfGitDir = GitUtil.findGitDir(gitRepository.getRoot());
    assert vfGitDir != null : "Can't get .git directory from repo root path ${gitRepository.getRoot()}";
    return vfGitDir;
  }

  /**
   * @param gitRepository {@link GitRepository} to get the virtual file from
   * @return an option of {@link VirtualFile} representing the machete file
   *
   */
  public static Option<VirtualFile> getMacheteFile(GitRepository gitRepository) {
    return Option.of(getGitDirectory(gitRepository).findChild(MacheteFileType.FILE_NAME));
  }

  /**
   * In contrary to {@link GitVfsUtils#getMacheteFile(GitRepository)} the result always exists.
   * This is because the path is valid even though the file may not exist.
   * @param gitRepository {@link GitRepository} to resolve the path within
   * @return an option of {@link VirtualFile} representing the machete file
   */
  public static Path resolveMacheteFilePath(GitRepository gitRepository) {
    return Path.of(getGitDirectory(gitRepository).getPath()).resolve(MacheteFileType.FILE_NAME);
  }
}
