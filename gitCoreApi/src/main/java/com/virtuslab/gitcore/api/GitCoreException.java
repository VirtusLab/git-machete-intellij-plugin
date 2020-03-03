package com.virtuslab.gitcore.api;

public class GitCoreException extends Exception {
  public GitCoreException() {
    super();
  }

  public GitCoreException(String message) {
    super(message);
  }

  public GitCoreException(String message, Throwable cause) {
    super(message, cause);
  }

  public GitCoreException(Throwable cause) {
    super(cause);
  }
}
