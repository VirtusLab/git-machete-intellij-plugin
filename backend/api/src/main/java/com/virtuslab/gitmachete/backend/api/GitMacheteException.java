package com.virtuslab.gitmachete.backend.api;

public class GitMacheteException extends Exception {
  public GitMacheteException() {
    super();
  }

  public GitMacheteException(String message) {
    super(message);
  }

  public GitMacheteException(Throwable e) {
    super(e);
  }

  public GitMacheteException(String message, Throwable e) {
    super(message, e);
  }

  public static GitMacheteException getOrWrap(Throwable e) {
    return e instanceof GitMacheteException ? (GitMacheteException) e : new GitMacheteException(e);
  }
}
