package com.virtuslab.branchlayout.api;

import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BranchLayoutException extends Exception {

  private final @Nullable @Positive Integer errorLine;

  public BranchLayoutException(@Nullable @Positive Integer errorLine, String message) {
    super(message);
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

  public @Nullable @Positive Integer getErrorLine() {
    return errorLine;
  }

  @Override
  public String toString() {
    return super.toString() + (errorLine != null ? ". Problematic line number: " + errorLine : "");
  }
}
