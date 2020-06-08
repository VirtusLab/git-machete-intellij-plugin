package com.virtuslab.gitcore.impl.jgit;

import java.time.Instant;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.aliasing.qual.NonLeaked;
import org.eclipse.jgit.revwalk.RevCommit;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;

@Getter
public class GitCoreCommit implements IGitCoreCommit {
  private final String shortMessage;
  private final GitCorePersonIdentity author;
  private final GitCorePersonIdentity committer;
  private final Instant commitTime;
  private final IGitCoreCommitHash hash;

  public GitCoreCommit(@NonLeaked RevCommit commit) {
    this.shortMessage = commit.getShortMessage();
    this.author = GitCorePersonIdentity.of(commit.getAuthorIdent());
    this.committer = GitCorePersonIdentity.of(commit.getCommitterIdent());
    this.commitTime = Instant.ofEpochSecond(commit.getCommitTime());
    this.hash = GitCoreCommitHash.of(commit.getId());
  }

  @Override
  public String toString() {
    return hash.getShortHashString() + ": '" + shortMessage + "'";
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    return IGitCoreCommit.defaultEquals(this, other);
  }

  @Override
  public final int hashCode() {
    return IGitCoreCommit.defaultHashCode(this);
  }
}
