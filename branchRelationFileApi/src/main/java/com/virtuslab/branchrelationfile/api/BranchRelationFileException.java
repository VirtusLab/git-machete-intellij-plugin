package com.virtuslab.branchrelationfile.api;

import java.util.Optional;
import lombok.Getter;

public class BranchRelationFileException extends Exception {
  @Getter private Optional<Integer> errorLine = Optional.empty();

  public BranchRelationFileException() {
    super();
  }

  public BranchRelationFileException(int errorLine) {
    super();
    this.errorLine = Optional.of(errorLine);
  }

  public BranchRelationFileException(String message) {
    super(message);
  }

  public BranchRelationFileException(String message, int errorLine) {
    super(message);
    this.errorLine = Optional.of(errorLine);
  }

  public BranchRelationFileException(Throwable e) {
    super(e);
  }

  public BranchRelationFileException(Throwable e, int errorLine) {
    super(e);
    this.errorLine = Optional.of(errorLine);
  }

  public BranchRelationFileException(String message, Throwable e) {
    super(message, e);
  }

  public BranchRelationFileException(String message, Throwable e, int errorLine) {
    super(message, e);
    this.errorLine = Optional.of(errorLine);
  }
}
