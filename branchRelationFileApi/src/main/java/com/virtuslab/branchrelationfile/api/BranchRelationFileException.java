package com.virtuslab.branchrelationfile.api;

import java.util.Optional;

public class BranchRelationFileException extends Exception {
  private final Integer errorLine;

  public BranchRelationFileException() {
    super();
    this.errorLine = null;
  }

  public BranchRelationFileException(int errorLine) {
    super();
    this.errorLine = errorLine;
  }

  public BranchRelationFileException(String message) {
    super(message);
    this.errorLine = null;
  }

  public BranchRelationFileException(String message, int errorLine) {
    super(message);
    this.errorLine = errorLine;
  }

  public BranchRelationFileException(Throwable e) {
    super(e);
    this.errorLine = null;
  }

  public BranchRelationFileException(Throwable e, int errorLine) {
    super(e);
    this.errorLine = errorLine;
  }

  public BranchRelationFileException(String message, Throwable e) {
    super(message, e);
    this.errorLine = null;
  }

  public BranchRelationFileException(String message, Throwable e, int errorLine) {
    super(message, e);
    this.errorLine = errorLine;
  }

  public Optional<Integer> getErrorLine() {
    return Optional.ofNullable(errorLine);
  }
}
