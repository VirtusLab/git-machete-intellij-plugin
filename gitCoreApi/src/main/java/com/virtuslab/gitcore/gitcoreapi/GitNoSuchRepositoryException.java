package com.virtuslab.gitcore.gitcoreapi;

public class GitNoSuchRepositoryException extends GitException {
  public GitNoSuchRepositoryException() {
    super();
  }

  public GitNoSuchRepositoryException(String message) {
    super(message);
  }

  public GitNoSuchRepositoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public GitNoSuchRepositoryException(Throwable cause) {
    super(cause);
  }
}
