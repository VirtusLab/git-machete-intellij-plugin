package com.virtuslab.gitmachete.backend.api;

public class MacheteFileReaderException extends GitMacheteJGitException {
  public MacheteFileReaderException() {
    super();
  }

  public MacheteFileReaderException(String message) {
    super(message);
  }

  public MacheteFileReaderException(Throwable e) {
    super(e);
  }

  public MacheteFileReaderException(String message, Throwable e) {
    super(message, e);
  }
}
