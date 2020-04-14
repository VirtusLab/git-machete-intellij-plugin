package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import com.virtuslab.gitcore.api.BaseGitCoreBranch;
import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchBranchException;
import com.virtuslab.gitcore.api.GitCoreNoSuchCommitException;
import com.virtuslab.gitcore.api.IGitCoreBranch;

@Getter
@RequiredArgsConstructor
@ToString
public abstract class GitCoreBranch extends BaseGitCoreBranch {
  protected final GitCoreRepository repo;
  protected final String branchName;

  private final AtomicReference<GitCoreCommit> pointedCommitRef = new AtomicReference<>(null);

  @Override
  public String getName() {
    return branchName;
  }

  public String getFullName() {
    return getBranchesPath() + branchName;
  }

  public abstract String getBranchesPath();

  public abstract String getBranchTypeString();

  public abstract String getBranchTypeString(boolean capitalized);

  @Override
  public GitCoreCommit getPointedCommit() throws GitCoreException {
    GitCoreCommit gitCoreCommit = pointedCommitRef.get();
    if (gitCoreCommit == null) {
      GitCoreCommit value = new GitCoreCommit(derivePointedRevCommit());
      pointedCommitRef.set(value);
      return value;
    }
    return gitCoreCommit;
  }

  protected RevCommit derivePointedRevCommit() throws GitCoreException {
    Repository jgitRepo = repo.getJgitRepo();
    RevWalk rw = new RevWalk(jgitRepo);
    RevCommit c;
    try {
      ObjectId o = jgitRepo.resolve(getFullName());
      if (o == null) {
        throw new GitCoreNoSuchBranchException(
            "${getBranchTypeString(/* capitalized */ true)} branch '${branchName}' does not exist in this repository");
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
  public Optional<BaseGitCoreCommit> deriveMergeBase(IGitCoreBranch branch) throws GitCoreException {
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
      assert objectId != null : "objectId is null";
      walk.markStart(walk.parseCommit(objectId));
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

    Set<ObjectId> ancestorsOfStartCommits = new HashSet<>();

    // basically looking for a first repeated element in the list
    return Iterator.ofAll(walk)
        .toStream()
        .map(RevCommit::getParents)
        .flatMap(Stream::of)
        .find(e -> !ancestorsOfStartCommits.add(e))
        .<BaseGitCoreCommit>map(GitCoreCommit::new)
        .toJavaOptional();
  }

  @Override
  public List<BaseGitCoreCommit> deriveCommitsUntil(BaseGitCoreCommit upToCommit) throws GitCoreException {
    RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    RevCommit commit = derivePointedRevCommit();

    RevWalk revWalk = Try.of(() -> {
      walk.markStart(commit);
      return walk;
    }).getOrElseThrow(e -> new GitCoreException(e));

    return Iterator.ofAll(revWalk)
        .takeUntil(revCommit -> revCommit.getId().getName().equals(upToCommit.getHash().getHashString()))
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
}
