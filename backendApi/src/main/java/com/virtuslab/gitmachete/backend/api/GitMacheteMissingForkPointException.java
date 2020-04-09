package com.virtuslab.gitmachete.backend.api;

public class GitMacheteMissingForkPointException extends GitMacheteException {
  public GitMacheteMissingForkPointException() {
    super();
  }

  public GitMacheteMissingForkPointException(String message) {
    super(message);
  }

  public GitMacheteMissingForkPointException(Throwable e) {
    super(e);
  }

  public GitMacheteMissingForkPointException(String message, Throwable e) {
    super(message, e);
  }
}
