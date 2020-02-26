package com.virtuslab.gitcore.impl.jgit;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import java.io.IOException;
import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class GitCoreCommit implements IGitCoreCommit {
  private final RevCommit jgitCommit;
  // TODO (#93): separate (and reimplement) isAncestorOf logic, remove the following field
  private final GitCoreRepository repo;
  private final String message;
  private final GitCorePersonIdentity author;
  private final GitCorePersonIdentity committer;
  private final Date commitTime;
  @EqualsAndHashCode.Include private final GitCoreCommitHash hash;

  public GitCoreCommit(RevCommit commit, GitCoreRepository repo) {
    if (commit == null)
      throw new NullPointerException("JGit commit passed to Commit constructor cannot be null");
    if (repo == null)
      throw new NullPointerException("JGit repository passed to Commit constructor cannot be null");
    this.jgitCommit = commit;
    this.repo = repo;
    this.message = jgitCommit.getFullMessage();
    this.author = new GitCorePersonIdentity(jgitCommit.getAuthorIdent());
    this.committer = new GitCorePersonIdentity(jgitCommit.getCommitterIdent());
    this.commitTime = new Date(jgitCommit.getCommitTime());
    this.hash = new GitCoreCommitHash(jgitCommit.getId().getName());
  }

  @Override
  public boolean isAncestorOf(IGitCoreCommit parentCommit) throws GitCoreException {
    var jgitRepo = repo.getJgitRepo();
    RevWalk walk = new RevWalk(jgitRepo);
    walk.sort(RevSort.TOPO);
    try {
      walk.markStart(walk.parseCommit(jgitRepo.resolve(parentCommit.getHash().getHashString())));
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

    for (var c : walk) {
      if (c.getId().equals(jgitCommit.getId())) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String toString() {
    return jgitCommit.getId().getName().substring(0, 7) + ": " + jgitCommit.getShortMessage();
  }
}
