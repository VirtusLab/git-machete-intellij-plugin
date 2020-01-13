package com.virtuslab.branchrelationfile;

public class BranchRelationFileException extends Exception {
  public BranchRelationFileException() {
    super();
  }

  public BranchRelationFileException(String message) {
    super(message);
  }

  public BranchRelationFileException(Throwable e) {
    super(e);
  }

  public BranchRelationFileException(String message, Throwable e) {
    super(message, e);
  }
}
