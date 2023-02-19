package com.virtuslab.gitcore.api;

import lombok.experimental.StandardException;

@StandardException
@SuppressWarnings("nullness:argument")
public class GitCoreException extends Exception {
  public static GitCoreException getOrWrap(Throwable e) {
    return e instanceof GitCoreException ? (GitCoreException) e : new GitCoreException(e);
  }
}
