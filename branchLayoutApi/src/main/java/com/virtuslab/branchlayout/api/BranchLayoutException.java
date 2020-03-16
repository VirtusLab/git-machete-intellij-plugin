package com.virtuslab.branchlayout.api;

import java.util.Optional;

public class BranchLayoutException extends Exception {
  private final Integer errorLine;

  public BranchLayoutException(Integer errorLine, String message) {
    super(message);
    this.errorLine = errorLine;
  }

  public BranchLayoutException(String message) {
    this(null, message);
  }

  public BranchLayoutException(Integer errorLine, String message, Throwable e) {
    super(message, e);
    this.errorLine = errorLine;
  }

  public BranchLayoutException(String message, Throwable e) {
    this(null, message, e);
  }

  public Optional<Integer> getErrorLine() {
    return Optional.ofNullable(errorLine);
  }
}
