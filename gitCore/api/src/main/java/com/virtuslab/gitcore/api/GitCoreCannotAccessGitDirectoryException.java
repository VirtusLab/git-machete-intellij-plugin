package com.virtuslab.gitcore.api;

public class GitCoreCannotAccessGitDirectoryException extends GitCoreException {
  public GitCoreCannotAccessGitDirectoryException() {
    super();
  }

  public GitCoreCannotAccessGitDirectoryException(String message) {
    super(message);
  }

  public GitCoreCannotAccessGitDirectoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public GitCoreCannotAccessGitDirectoryException(Throwable cause) {
    super(cause);
  }
}
