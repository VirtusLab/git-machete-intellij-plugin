package com.virtuslab.gitcore.impl.jgit;

import java.util.Date;

import lombok.Getter;

import org.eclipse.jgit.revwalk.RevCommit;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;

@Getter
public class GitCoreCommit extends BaseGitCoreCommit {
  private final RevCommit jgitCommit;
  private final String message;
  private final GitCorePersonIdentity author;
  private final GitCorePersonIdentity committer;
  private final Date commitTime;
  private final String hashString;

  public GitCoreCommit(RevCommit commit) {
    if (commit == null)
      throw new NullPointerException("JGit commit passed to Commit constructor cannot be null");
    this.jgitCommit = commit;
    this.message = jgitCommit.getFullMessage();
    this.author = new GitCorePersonIdentity(jgitCommit.getAuthorIdent());
    this.committer = new GitCorePersonIdentity(jgitCommit.getCommitterIdent());
    this.commitTime = new Date((long) jgitCommit.getCommitTime() * 1000);
    this.hashString = jgitCommit.getId().getName();
  }

  @Override
  public String toString() {
    return jgitCommit.getId().getName().substring(0, 7) + ": " + jgitCommit.getShortMessage();
  }
}
