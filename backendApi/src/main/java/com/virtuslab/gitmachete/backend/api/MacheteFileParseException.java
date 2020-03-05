package com.virtuslab.gitmachete.backend.api;

public class MacheteFileParseException extends GitMacheteJGitException {
  public MacheteFileParseException() {
    super();
  }

  public MacheteFileParseException(String message) {
    super(message);
  }

  public MacheteFileParseException(Throwable e) {
    super(e);
  }

  public MacheteFileParseException(String message, Throwable e) {
    super(message, e);
  }
}
