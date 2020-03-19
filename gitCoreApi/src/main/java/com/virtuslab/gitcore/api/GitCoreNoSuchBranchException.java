package com.virtuslab.gitcore.api;

import java.util.HashSet;
public class GitCoreNoSuchBranchException extends GitCoreException {
  public GitCoreNoSuchBranchException() {
    super();
  }

  public GitCoreNoSuchBranchException(String message) {
    super(message);
  }

  public GitCoreNoSuchBranchException(String message, Throwable cause) {
    super(message, cause);
  }

  public GitCoreNoSuchBranchException(Throwable cause) {
    super(cause);
  }
}
