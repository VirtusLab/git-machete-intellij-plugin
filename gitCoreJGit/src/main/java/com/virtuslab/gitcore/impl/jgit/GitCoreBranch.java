package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.control.Try;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchBranchException;
import com.virtuslab.gitcore.api.GitCoreNoSuchCommitException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreCommit;

@Data
public abstract class GitCoreBranch implements IGitCoreBranch {
  protected final GitCoreRepository repo;
  protected final String branchName;

  @Override
  public String getName() {
    return branchName;
  }

  @EqualsAndHashCode.Include
  public String getFullName() {
    return getBranchesPath() + branchName;
  }

  public abstract String getBranchesPath();

  public abstract String getBranchTypeString();

  public abstract String getBranchTypeString(boolean capitalized);

  @Override
  public GitCoreCommit getPointedCommit() throws GitCoreException {
    return new GitCoreCommit(computePointedRevCommit());
  }

  protected RevCommit computePointedRevCommit() throws GitCoreException {
    Repository jgitRepo = repo.getJgitRepo();
    RevWalk rw = new RevWalk(jgitRepo);
    RevCommit c;
    try {
      ObjectId o = jgitRepo.resolve(getFullName());
      if (o == null) {
        throw new GitCoreNoSuchBranchException(
            MessageFormat.format("{1} branch \"{0}\" does not exist in this repository", branchName,
                getBranchTypeString(/* capitalized */ true)));
      }
      c = rw.parseCommit(o);
    } catch (MissingObjectException | IncorrectObjectTypeException e) {
      throw new GitCoreNoSuchCommitException(MessageFormat.format(
          "Commit pointed by {1} branch \"{0}\" does not exist in this repository", branchName, getBranchTypeString()));
    } catch (RevisionSyntaxException | IOException e) {
      throw new GitCoreException(e);
    }

    return c;
  }

  @Override
  public Optional<IGitCoreCommit> computeMergeBase(IGitCoreBranch branch) throws GitCoreException {
    RevWalk walk = new RevWalk(repo.getJgitRepo());

    walk.sort(RevSort.TOPO, /* use */ true);
    walk.sort(RevSort.COMMIT_TIME_DESC, /* use */ true);

    try {
      /*
       * I mark both commits as a start commit, because I want to traverse tree of commits starting from that points. In
       * every iteration iterator from "walk" give me next commit from one of this path depend on the commit date. In
       * every iteration of iterator I try add current commit's parent(s) (actually their ObjectId's) to
       * "ancestorsOfStartCommits" list. If one of parent's ObjectId is already in this list, that mean (in
       * consideration of this sorting) that it's a merge base (the first common ancestor)
       */
      walk.markStart(this.getPointedCommit().getJgitCommit());

      String commitHash = branch.getPointedCommit().getHash().getHashString();
      ObjectId objectId = repo.getJgitRepo().resolve(commitHash);
      walk.markStart(walk.parseCommit(objectId));
    } catch (Exception e) {
      throw new GitCoreException(e);
    }

    Set<ObjectId> ancestorsOfStartCommits = new HashSet<>();
    return Iterator.ofAll(walk)
        .toStream()
        .map(RevCommit::getParents)
        .map(List::of)
        .flatMap(i -> i)
        .peek(ancestorsOfStartCommits::add)
        .filter(ancestorsOfStartCommits::contains)
        .map(ref -> (IGitCoreCommit) new GitCoreCommit(ref))
        .headOption()
        .toJavaOptional();
  }

  @Override
  public List<IGitCoreCommit> computeCommitsUntil(IGitCoreCommit upToCommit) throws GitCoreException {
    RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    RevCommit commit = computePointedRevCommit();

    return Try.of(() -> {
      walk.markStart(commit);
      return walk;
    }).map(Iterator::ofAll)
        .getOrElseThrow(e -> new GitCoreException(e))
        .takeUntil(revCommit -> revCommit.getId().getName().equals(upToCommit.getHash().getHashString()))
        .map(GitCoreCommit::new)
        .collect(List.collector());
  }

  @Override
  public boolean hasJustBeenCreated() throws GitCoreException {
    return Try.of(() -> repo.getJgitGit().reflog().setRef(getFullName()).call())
        .map(List::ofAll)
        .getOrElseThrow(e -> new GitCoreException(e))
        .map(entry -> entry.getOldId().equals(ObjectId.zeroId()))
        .getOrElse(true);
  }
}
