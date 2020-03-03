package com.virtuslab.gitcore.api;

public class GitCoreNoSuchRepositoryException extends GitCoreException {
  public GitCoreNoSuchRepositoryException() {
    super();
  }

  public GitCoreNoSuchRepositoryException(String message) {
    super(message);
  }

  public GitCoreNoSuchRepositoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public GitCoreNoSuchRepositoryException(Throwable cause) {
    super(cause);
  }
}
