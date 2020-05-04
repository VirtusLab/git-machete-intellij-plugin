package com.virtuslab.gitcore.impl.jgit;

import java.util.Date;

import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.BaseGitCoreCommitHash;

@Getter
public class GitCoreCommit extends BaseGitCoreCommit {
  private final RevCommit jgitCommit;
  private final String message;
  private final GitCorePersonIdentity author;
  private final GitCorePersonIdentity committer;
  private final Date commitDate;
  private final BaseGitCoreCommitHash hash;

  public GitCoreCommit(RevCommit commit) {
    this.jgitCommit = commit;
    this.message = jgitCommit.getFullMessage();
    this.author = new GitCorePersonIdentity(jgitCommit.getAuthorIdent());
    this.committer = new GitCorePersonIdentity(jgitCommit.getCommitterIdent());
    this.commitDate = new Date((long) jgitCommit.getCommitTime() * 1000);
    this.hash = GitCoreCommitHash.of(jgitCommit);
  }

  @Override
  @SuppressWarnings("index:argument.type.incompatible")
  public String toString() {
    return jgitCommit.getId().getName().substring(0, 7) + ": " + jgitCommit.getShortMessage();
  }
}
