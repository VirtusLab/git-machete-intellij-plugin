package com.virtuslab.gitcore.api;

public class GitCoreCannotReadGitFileException extends GitCoreException {
  public GitCoreCannotReadGitFileException() {
    super();
  }

  public GitCoreCannotReadGitFileException(String message) {
    super(message);
  }

  public GitCoreCannotReadGitFileException(String message, Throwable cause) {
    super(message, cause);
  }

  public GitCoreCannotReadGitFileException(Throwable cause) {
    super(cause);
  }
}
