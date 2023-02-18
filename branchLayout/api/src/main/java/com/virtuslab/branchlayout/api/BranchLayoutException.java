package com.virtuslab.branchlayout.api;

import lombok.Getter;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BranchLayoutException extends Exception {

  @Getter
  private final @Nullable @Positive Integer errorLine;

  public BranchLayoutException(@Nullable @Positive Integer errorLine, String message) {
    super(message + (errorLine != null ? ". Problematic line number: " + errorLine : ""));
    this.errorLine = errorLine;
  }

  public BranchLayoutException(String message) {
    this(null, message);
  }

  public BranchLayoutException(@Nullable @Positive Integer errorLine, String message, Throwable e) {
    super(message, e);
    this.errorLine = errorLine;
  }

  public BranchLayoutException(String message, Throwable e) {
    this(null, message, e);
  }
}
