package com.virtuslab.gitcore.impl.jgit;

import java.time.Instant;

import lombok.Getter;
import org.checkerframework.common.aliasing.qual.NonLeaked;
import org.eclipse.jgit.revwalk.RevCommit;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.BaseGitCoreCommitHash;

@Getter
public class GitCoreCommit extends BaseGitCoreCommit {
  private final String message;
  private final GitCorePersonIdentity author;
  private final GitCorePersonIdentity committer;
  private final Instant commitTime;
  private final BaseGitCoreCommitHash hash;
  private final String stringValue;

  @SuppressWarnings("index:argument.type.incompatible")
  public GitCoreCommit(@NonLeaked RevCommit commit) {
    this.message = commit.getFullMessage();
    this.author = new GitCorePersonIdentity(commit.getAuthorIdent());
    this.committer = new GitCorePersonIdentity(commit.getCommitterIdent());
    this.commitTime = Instant.ofEpochSecond(commit.getCommitTime());
    this.hash = new GitCoreCommitHash(commit.getId().getName());
    this.stringValue = commit.getId().getName().substring(0, 7) + ": " + commit.getShortMessage();
  }

  @Override
  public String toString() {
    return stringValue;
  }
}
