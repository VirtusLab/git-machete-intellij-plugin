package com.virtuslab.gitmachete.file;

public class GitMacheteFileException extends Exception {
  public GitMacheteFileException() {
    super();
  }

  public GitMacheteFileException(String message) {
    super(message);
  }

  public GitMacheteFileException(Throwable e) {
    super(e);
  }

  public GitMacheteFileException(String message, Throwable e) {
    super(message, e);
  }
}
