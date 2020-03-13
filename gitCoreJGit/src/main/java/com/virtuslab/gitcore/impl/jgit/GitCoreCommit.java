package com.virtuslab.gitcore.impl.jgit;

import java.util.Date;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.eclipse.jgit.revwalk.RevCommit;

import com.virtuslab.gitcore.api.IGitCoreCommit;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class GitCoreCommit implements IGitCoreCommit {
  private final RevCommit jgitCommit;
  private final String message;
  private final GitCorePersonIdentity author;
  private final GitCorePersonIdentity committer;
  private final Date commitTime;
  @EqualsAndHashCode.Include
  private final GitCoreCommitHash hash;

  public GitCoreCommit(RevCommit commit) {
    if (commit == null)
      throw new NullPointerException("JGit commit passed to Commit constructor cannot be null");
    this.jgitCommit = commit;
    this.message = jgitCommit.getFullMessage();
    this.author = new GitCorePersonIdentity(jgitCommit.getAuthorIdent());
    this.committer = new GitCorePersonIdentity(jgitCommit.getCommitterIdent());
    this.commitTime = new Date((long) jgitCommit.getCommitTime() * 1000);
    this.hash = new GitCoreCommitHash(jgitCommit.getId().getName());
  }

  @Override
  public String toString() {
    return jgitCommit.getId().getName().substring(0, 7) + ": " + jgitCommit.getShortMessage();
  }
}
