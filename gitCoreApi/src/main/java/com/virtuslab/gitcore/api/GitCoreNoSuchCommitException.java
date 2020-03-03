package com.virtuslab.gitcore.api;

public class GitCoreNoSuchCommitException extends GitCoreException {
  public GitCoreNoSuchCommitException() {
    super();
  }

  public GitCoreNoSuchCommitException(String message) {
    super(message);
  }

  public GitCoreNoSuchCommitException(String message, Throwable cause) {
    super(message, cause);
  }

  public GitCoreNoSuchCommitException(Throwable cause) {
    super(cause);
  }
}
