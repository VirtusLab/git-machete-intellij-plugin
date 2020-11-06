package com.virtuslab.gitmachete.frontend.vfsutils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import io.vavr.control.Option;
import io.vavr.control.Try;

public final class GitVfsUtils {

  private static final String MACHETE_FILE_NAME = "machete";

  private GitVfsUtils() {}

  public static VirtualFile getGitDirectory(GitRepository gitRepository) {
    VirtualFile vfGitDir = GitUtil.findGitDir(gitRepository.getRoot());
    assert vfGitDir != null : "Can't get .git directory from repo root path ${gitRepository.getRoot()}";
    return vfGitDir;
  }

  public static Path getGitDirectoryPath(GitRepository gitRepository) {
    VirtualFile vfGitDir = getGitDirectory(gitRepository);
    return Path.of(vfGitDir.getPath());
  }

  public static VirtualFile getMainDirectory(GitRepository gitRepository) {
    return gitRepository.getRoot();
  }

  public static Path getMainDirectoryPath(GitRepository gitRepository) {
    return Path.of(getMainDirectory(gitRepository).getPath());
  }

  /**
   * @param gitRepository {@link GitRepository} to get the virtual file from
   * @return an option of {@link VirtualFile} representing the machete file
   */
  public static Option<VirtualFile> getMacheteFile(GitRepository gitRepository) {
    return Option.of(getGitDirectory(gitRepository).findChild(MACHETE_FILE_NAME));
  }

  /**
   * As opposed to {@link GitVfsUtils#getMacheteFile(GitRepository)} the result always exists.
   * This is because the path is valid even though the file may not exist.
   *
   * @param gitRepository {@link GitRepository} to resolve the path within
   * @return {@link Path} representing the machete file
   */
  public static Path getMacheteFilePath(GitRepository gitRepository) {
    return getGitDirectoryPath(gitRepository).resolve(MACHETE_FILE_NAME);
  }

  /**
   * @param filePath {@link Path} to file
   * @return an option of {@link Long} stating for time of last modification in milliseconds since the Unix epoch start
   */
  public static Option<Long> getFileModificationDate(Path filePath) {
    return Try.of(() -> Files.readAttributes(filePath, BasicFileAttributes.class))
        .map(attr -> attr.lastModifiedTime().toMillis()).toOption();
  }

  /**
   * @param filePath {@link Path} to file
   * @param millis {@code long} representing the new modification time in milliseconds since the Unix epoch start
   * @return an option of {@link Path} stating for the given file
   */
  public static Option<Path> setFileModificationDate(Path filePath, long millis) {
    return Try.of(() -> Files.setLastModifiedTime(filePath, FileTime.fromMillis(millis))).toOption();
  }
}
