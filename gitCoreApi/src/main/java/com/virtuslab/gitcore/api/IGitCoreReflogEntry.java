package com.virtuslab.gitcore.api;

import io.vavr.control.Option;
import org.checkerframework.dataflow.qual.Pure;

public interface IGitCoreReflogEntry {
  @Pure
  String getComment();

  @Pure
  Option<IGitCoreCommitHash> getOldCommitHash();

  @Pure
  IGitCoreCommitHash getNewCommitHash();
}
