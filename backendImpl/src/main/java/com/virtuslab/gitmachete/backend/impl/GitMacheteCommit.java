package com.virtuslab.gitmachete.backend.impl;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.ArrayLen;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;

@Getter
@RequiredArgsConstructor
@ToString
public class GitMacheteCommit implements IGitMacheteCommit {
  private final String shortMessage;
  private final @ArrayLen(40) String hash;
  private final @ArrayLen(7) String shortHash;
  private final Instant commitTime;

  public GitMacheteCommit(IGitCoreCommit coreCommit) {
    shortMessage = coreCommit.getShortMessage();
    hash = coreCommit.getHash().getHashString();
    shortHash = coreCommit.getHash().getShortHashString();
    commitTime = coreCommit.getCommitTime();
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
