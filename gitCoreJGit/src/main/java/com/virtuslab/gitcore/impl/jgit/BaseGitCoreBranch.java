package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.util.Collection;

import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.aliasing.qual.Unique;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreCommit;

@CustomLog
@Getter
@RequiredArgsConstructor
public abstract class BaseGitCoreBranch implements IGitCoreBranch {

  protected final GitCoreRepository repo;
  protected final String branchName;
  protected final String remoteName;

  @MonotonicNonNull
  private GitCoreCommit pointedCommit = null;

  @Override
  public String getName() {
    return branchName;
  }

  public abstract String getFullName();

  public abstract String getBranchesPath();

  public final String getBranchTypeString() {
    return getBranchTypeString(/* capitalized */ false);
  }

  public abstract String getBranchTypeString(boolean capitalized);

  @SuppressWarnings("regexp") // to allow `synchronized`
  @Override
  public synchronized GitCoreCommit getPointedCommit() throws GitCoreException {
    if (pointedCommit == null) {
      @Unique RevCommit revCommit = repo.revStringToRevCommit(getFullName());
      pointedCommit = new GitCoreCommit(revCommit);
    }
    return pointedCommit;
  }

  protected @Unique RevWalk getTopoRevWalkFromPointedCommit() throws GitCoreException {
    @Unique RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    @Unique RevCommit commit = repo.gitCoreCommitToRevCommit(getPointedCommit());
    try {
      walk.markStart(commit);
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
    return walk;
  }

  @Override
  public List<IGitCoreCommit> deriveCommitsUntil(IGitCoreCommit upToCommit) throws GitCoreException {
    LOG.debug(() -> "Entering: branch = '${getFullName()}', upToCommit = ${upToCommit}");

    LOG.debug("Starting revwalk");
    return Iterator.ofAll(getTopoRevWalkFromPointedCommit())
        .takeUntil(revCommit -> revCommit.getId().getName().equals(upToCommit.getHash().getHashString()))
        .map(revCommit -> {
          LOG.debug(() -> revCommit.getId().getName());
          return revCommit;
        })
        .map(GitCoreCommit::new)
        .collect(List.collector());
  }

  @Override
  public boolean hasJustBeenCreated() throws GitCoreException {
    Collection<ReflogEntry> rf = Try.of(() -> repo.getJgitGit().reflog().setRef(getFullName()).call())
        .getOrElseThrow(e -> new GitCoreException(e));

    var rfit = rf.iterator();

    if (!rfit.hasNext()) {
      return true;
    }

    return rfit.next().getOldId().equals(ObjectId.zeroId());
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof BaseGitCoreBranch)) {
      return false;
    } else {
      var o = (BaseGitCoreBranch) other;
      return getFullName().equals(o.getFullName())
          && Try.of(() -> getPointedCommit().equals(o.getPointedCommit())).getOrElse(false);
    }
  }

  @Override
  public final int hashCode() {
    return getFullName().hashCode() * 37 + Try.of(() -> getPointedCommit().hashCode()).getOrElse(0);
  }
}
