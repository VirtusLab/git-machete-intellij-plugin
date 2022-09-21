package com.virtuslab.gitcore.api;

import java.time.Instant;

import io.vavr.control.Option;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public interface IGitCoreReflogEntry {
  @Pure
  String getComment();

  @Pure
  Instant getTimestamp();

  @Pure
  @Nullable
  IGitCoreCommitHash getOldCommitHash();

  @Pure
  IGitCoreCommitHash getNewCommitHash();

  /**
   * @return an {@link Option.Some} with a {@link IGitCoreCheckoutEntry} if this reflog entry corresponds to a checkout;
   *         otherwise, an {@link Option.None}
   */
  @Nullable
  IGitCoreCheckoutEntry parseCheckout();
}
