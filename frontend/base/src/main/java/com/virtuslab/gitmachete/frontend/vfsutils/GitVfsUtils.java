package com.virtuslab.gitmachete.frontend.vfsutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;

public final class GitVfsUtils {

  private static final String MACHETE_FILE_NAME = "machete";

  private GitVfsUtils() {}

  public static VirtualFile getMainGitDirectory(GitRepository gitRepository) {
    VirtualFile vfGitDir = GitUtil.findGitDir(gitRepository.getRoot());
    assert vfGitDir != null : "Can't get .git directory from repo root path ${gitRepository.getRoot()}";
    // For worktrees, the format of .git dir path is:
    //   <repo-root>/.git/worktrees/<per-worktree-directory>
    // Let's detect if that's what `findGitDir` returned;
    // if so, let's use the top-level <repo-root>/.git dir instead.
    // Note that for submodules we're okay with the path returned by `findGitDir`.
    VirtualFile parent1 = vfGitDir.getParent();
    if (parent1 != null && parent1.getName().equals("worktrees")) {
      VirtualFile parent2 = parent1.getParent();
      if (parent2 != null && parent2.getName().equals(".git")) {
        return parent2;
      }
    }
    return vfGitDir;
  }

  public static VirtualFile getWorktreeGitDirectory(GitRepository gitRepository) {
    VirtualFile vfGitDir = GitUtil.findGitDir(gitRepository.getRoot());
    assert vfGitDir != null : "Can't get .git directory from repo root path ${gitRepository.getRoot()}";
    return vfGitDir;
  }

  public static Path getMainGitDirectoryPath(GitRepository gitRepository) {
    VirtualFile vfGitDir = getMainGitDirectory(gitRepository);
    return Paths.get(vfGitDir.getPath());
  }

  public static Path getWorktreeGitDirectoryPath(GitRepository gitRepository) {
    VirtualFile vfGitDir = getWorktreeGitDirectory(gitRepository);
    return Paths.get(vfGitDir.getPath());
  }

  public static VirtualFile getRootDirectory(GitRepository gitRepository) {
    return gitRepository.getRoot();
  }

  public static Path getRootDirectoryPath(GitRepository gitRepository) {
    return Paths.get(getRootDirectory(gitRepository).getPath());
  }

  /**
   * @param gitRepository {@link GitRepository} to get the virtual file from
   * @return {@link VirtualFile} representing the machete file if found; otherwise, null
   */
  public static VirtualFile getMacheteFile(GitRepository gitRepository) {
    return getMainGitDirectory(gitRepository).findChild(MACHETE_FILE_NAME);
  }

  /**
   * As opposed to {@link GitVfsUtils#getMacheteFile(GitRepository)} the result always exists.
   * This is because the path is valid even though the file may not exist.
   *
   * @param gitRepository {@link GitRepository} to resolve the path within
   * @return {@link Path} representing the machete file
   */
  public static Path getMacheteFilePath(GitRepository gitRepository) {
    return getMainGitDirectoryPath(gitRepository).resolve(MACHETE_FILE_NAME);
  }

  /**
   * @param filePath {@link Path} to file
   * @return {@link Long} stating for time of last modification in milliseconds since the Unix epoch start if attributes were read successfully; otherwise, null
   */
  public static Long getFileModificationDate(Path filePath) {
    try {
      BasicFileAttributes fileAtributes = Files.readAttributes(filePath, BasicFileAttributes.class);
      return fileAtributes.lastModifiedTime().toMillis();
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * @param filePath {@link Path} to file
   * @param millis {@code long} representing the new modification time in milliseconds since the Unix epoch start
   * @return {@link Path} stating for the given file if the modification date was set successfully; otherwise, null
   */
  public static Path setFileModificationDate(Path filePath, long millis) {
    try {
      return Files.setLastModifiedTime(filePath, FileTime.fromMillis(millis));
    } catch (IOException e) {
      return null;
    }
  }
}
