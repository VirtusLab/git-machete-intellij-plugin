package com.virtuslab.gitcore.gitcorejgit;

import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitcore.gitcoreapi.IGitCoreCommit;
import java.io.IOException;
import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

@EqualsAndHashCode
public class JGitCommit implements IGitCoreCommit {
  @Getter private final RevCommit jgitCommit;
  // TODO (#93): separate (and reimplement) isAncestorOf logic, remove the following field
  private final JGitRepository repo;

  public JGitCommit(RevCommit commit, JGitRepository repo) {
    if (commit == null)
      throw new NullPointerException("JGit commit passed to Commit constructor cannot be null");
    if (repo == null)
      throw new NullPointerException("JGit repository passed to Commit constructor cannot be null");
    this.jgitCommit = commit;
    this.repo = repo;
  }

  @Override
  public String getMessage() {
    return jgitCommit.getFullMessage();
  }

  @Override
  public JGitPersonIdentity getAuthor() {
    return new JGitPersonIdentity(jgitCommit.getAuthorIdent());
  }

  @Override
  public JGitPersonIdentity getCommitter() {
    return new JGitPersonIdentity(jgitCommit.getCommitterIdent());
  }

  @Override
  public Date getCommitTime() {
    return new Date(jgitCommit.getCommitTime());
  }

  @Override
  public JGitCommitHash getHash() {
    return new JGitCommitHash(jgitCommit.getId().getName());
  }

  @Override
  public boolean isAncestorOf(IGitCoreCommit parentCommit) throws GitException {
    var jgitRepo = repo.getJgitRepo();
    RevWalk walk = new RevWalk(jgitRepo);
    walk.sort(RevSort.TOPO);
    try {
      walk.markStart(walk.parseCommit(jgitRepo.resolve(parentCommit.getHash().getHashString())));
    } catch (IOException e) {
      throw new JGitException(e);
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
