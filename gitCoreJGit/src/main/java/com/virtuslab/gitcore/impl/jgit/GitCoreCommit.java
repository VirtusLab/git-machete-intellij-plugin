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
  private final String message;
  private final GitCorePersonIdentity author;
  private final GitCorePersonIdentity committer;
  private final Instant commitTime;
  private final IGitCoreCommitHash hash;
  private final String stringValue;

  @SuppressWarnings("index:argument.type.incompatible")
  public GitCoreCommit(@NonLeaked RevCommit commit) {
    this.message = commit.getFullMessage();
    this.author = GitCorePersonIdentity.of(commit.getAuthorIdent());
    this.committer = GitCorePersonIdentity.of(commit.getCommitterIdent());
    this.commitTime = Instant.ofEpochSecond(commit.getCommitTime());
    this.hash = GitCoreCommitHash.of(commit.getId());
    this.stringValue = commit.getId().getName().substring(0, 7) + ": " + commit.getShortMessage();
  }

  @Override
  public String toString() {
    return stringValue;
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof IGitCoreCommit)) {
      return false;
    } else {
      return getHash().equals(((IGitCoreCommit) other).getHash());
    }
  }

  @Override
  public final int hashCode() {
    return getHash().hashCode();
  }
}
