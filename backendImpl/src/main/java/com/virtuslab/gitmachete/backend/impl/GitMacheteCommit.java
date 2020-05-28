package com.virtuslab.gitmachete.backend.impl;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.ArrayLen;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@ToString(onlyExplicitlyIncluded = true)
public class GitMacheteCommit implements IGitMacheteCommit {

  @Getter(AccessLevel.PACKAGE)
  private final IGitCoreCommit coreCommit;

  @Override
  @ToString.Include(name = "message")
  public String getShortMessage() {
    return coreCommit.getShortMessage();
  }

  @Override
  public @ArrayLen(40) String getHash() {
    return coreCommit.getHash().getHashString();
  }

  @Override
  @ToString.Include(name = "hash")
  public @ArrayLen(7) String getShortHash() {
    return coreCommit.getHash().getShortHashString();
  }

  @Override
  public Instant getCommitTime() {
    return coreCommit.getCommitTime();
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    return IGitMacheteCommit.defaultEquals(this, other);
  }

  @Override
  public final int hashCode() {
    return IGitMacheteCommit.defaultHashCode(this);
  }
}
