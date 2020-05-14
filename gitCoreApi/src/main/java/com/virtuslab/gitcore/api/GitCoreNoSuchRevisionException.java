package com.virtuslab.gitcore.api;

public class GitCoreNoSuchRevisionException extends GitCoreException {
  public GitCoreNoSuchRevisionException() {
    super();
  }

  public GitCoreNoSuchRevisionException(String message) {
    super(message);
  }

  public GitCoreNoSuchRevisionException(String message, Throwable cause) {
    super(message, cause);
  }

  public GitCoreNoSuchRevisionException(Throwable cause) {
    super(cause);
  }
}
