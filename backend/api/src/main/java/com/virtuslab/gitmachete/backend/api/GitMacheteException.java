package com.virtuslab.gitmachete.backend.api;

import lombok.experimental.StandardException;

@StandardException
@SuppressWarnings("nullness:argument")
public class GitMacheteException extends Exception {
  public static GitMacheteException getOrWrap(Throwable e) {
    return e instanceof GitMacheteException gme ? gme : new GitMacheteException(e);
  }
}
