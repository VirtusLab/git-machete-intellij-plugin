package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.util.Collection;

import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.aliasing.qual.Unique;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchCommitException;
import com.virtuslab.gitcore.api.GitCoreNoSuchRevisionException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.logger.EnhancedLambdaLoggerFactory;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@Getter
@RequiredArgsConstructor
public abstract class BaseGitCoreBranch implements IGitCoreBranch {
  private static final IEnhancedLambdaLogger LOG = EnhancedLambdaLoggerFactory.create();

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

  public abstract String getBranchTypeString();

  public abstract String getBranchTypeString(boolean capitalized);

  @SuppressWarnings("regexp") // to allow `synchronized`
  @Override
  public synchronized GitCoreCommit getPointedCommit() throws GitCoreException {
    if (pointedCommit == null) {
      var revStr = getFullName();
      @Unique RevCommit revCommit = resolveRevCommit(revStr);
      pointedCommit = new GitCoreCommit(revCommit);
    }
    return pointedCommit;
  }

  // - - - IMPORTANT NOTE - - -
  // Bear in mind that RevCommit is a mutable object.
  // Its internal state (inDegree, flags) changes during a rev walk (among others).
  // To avoid potential bugs:
  // - reinstantiate instead of reuse; this method provides a "clean" instance based on the given String revision
  // - narrow the scope where a RevCommit is available; a use as a field is strongly discouraged.
  // This comment applies everywhere in the codebase.
  // Note that both points are kind-of enforced by Checkstyle (every occurrence of "RevCommit" must be preceded with Checker's @Unique annotation),
  // but this is not perfect - for instance, it doesn't catch RevCommits declared as `var`s.
  protected @Unique RevCommit resolveRevCommit(String revStr) throws GitCoreException {
    Repository jgitRepo = repo.getJgitRepo();
    RevWalk rw = new RevWalk(jgitRepo);
    @Unique RevCommit c;
    try {
      ObjectId o = jgitRepo.resolve(revStr);
      if (o == null) {
        throw new GitCoreNoSuchRevisionException(
            "${getBranchTypeString(/* capitalized */ true)} branch '${branchName}', revision '${revStr}' does not exist in this repository");
      }
      c = rw.parseCommit(o);
    } catch (MissingObjectException | IncorrectObjectTypeException e) {
      throw new GitCoreNoSuchCommitException(
          "Commit pointed by ${getBranchTypeString()} branch '${branchName}' does not exist in this repository");
    } catch (RevisionSyntaxException | IOException e) {
      throw new GitCoreException(e);
    }
    return c;
  }

  @Override
  public List<IGitCoreCommit> deriveCommitsUntil(IGitCoreCommit upToCommit) throws GitCoreException {
    LOG.debug(() -> "Entering: branch = '${getFullName()}', upToCommit = ${upToCommit.getHash().getHashString()}");

    RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    @Unique RevCommit commit = resolveRevCommit(getPointedCommit().getHash().getHashString());

    RevWalk revWalk = Try.of(() -> {
      walk.markStart(commit);
      return walk;
    }).getOrElseThrow(e -> new GitCoreException(e));

    LOG.debug("Start revwalk");

    return Iterator.ofAll(revWalk)
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
