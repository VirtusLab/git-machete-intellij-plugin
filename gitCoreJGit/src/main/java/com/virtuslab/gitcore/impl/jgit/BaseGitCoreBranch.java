package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.util.function.Predicate;

import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.aliasing.qual.Unique;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.GitCoreNoSuchRevisionException;
import com.virtuslab.gitcore.api.IGitCoreBranch;
import com.virtuslab.gitcore.api.IGitCoreCommit;

@CustomLog
@RequiredArgsConstructor
public abstract class BaseGitCoreBranch implements IGitCoreBranch {

  protected final GitCoreRepository repo;
  protected final String branchName;
  protected final String remoteName;

  @MonotonicNonNull
  private GitCoreCommit pointedCommit = null;

  @MonotonicNonNull
  private List<ReflogEntry> filteredReflog = null;

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
    ObjectId objectId = repo.gitCoreCommitToObjectId(getPointedCommit());
    try {
      walk.markStart(walk.parseCommit(objectId));
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
    return walk;
  }

  @Override
  public List<IGitCoreCommit> deriveCommitsUntil(IGitCoreCommit upToCommit) throws GitCoreException {
    LOG.debug(() -> "Entering: branch = '${getFullName()}', upToCommit = ${upToCommit}");
    @Unique RevWalk walk = getTopoRevWalkFromPointedCommit();
    try {
      walk.markUninteresting(walk.parseCommit(repo.gitCoreCommitToObjectId(upToCommit)));
    } catch (IOException e) {
      throw new GitCoreException(e);
    }
    walk.sort(RevSort.BOUNDARY);

    LOG.debug("Starting revwalk");
    return Iterator.ofAll(walk.iterator())
        .takeUntil(revCommit -> revCommit.getId().getName().equals(upToCommit.getHash().getHashString()))
        .map(revCommit -> {
          LOG.debug(() -> "* " + revCommit.getId().getName());
          return revCommit;
        })
        .map(GitCoreCommit::new)
        .collect(List.collector());
  }

  private List<ReflogEntry> rejectExcludedEntries(List<ReflogEntry> entries) {
    LOG.trace(() -> "Entering: branch = '${getFullName()}'; original list of entries:");
    entries.forEach(entry -> LOG.trace(() -> "* ${entry}"));
    ObjectId entryToExcludeNewId;
    if (entries.size() > 0) {
      ReflogEntry firstEntry = entries.get(entries.size() - 1);
      String createdFromPrefix = "branch: Created from";
      if (firstEntry.getComment().startsWith(createdFromPrefix)) {
        entryToExcludeNewId = firstEntry.getNewId();
        LOG.debug(
            () -> "All entries with the same hash as first entry (${firstEntry.getNewId().toString()}) will be excluded "
                + "because first entry comment starts with '${createdFromPrefix}'");
      } else {
        entryToExcludeNewId = ObjectId.zeroId();
      }
    } else {
      entryToExcludeNewId = ObjectId.zeroId();
    }

    String rebaseComment = "rebase finished: " + getFullName() + " onto "
        + Try.of(() -> getPointedCommit().getHash().getHashString()).getOrElse("");

    // It's necessary to exclude entry with the same hash as the first entry in reflog (if it still exists)
    // for cases like branch rename just after branch creation
    Predicate<ReflogEntry> isEntryExcluded = e -> {
      // For debug logging only
      String newIdHash = e.getNewId().getName();

      if (e.getNewId().equals(entryToExcludeNewId)) {
        LOG.debug(() -> "Exclude ${newIdHash} because it has the same hash as first entry");
      } else if (e.getNewId().equals(e.getOldId())) {
        LOG.debug(() -> "Exclude ${newIdHash} because its old and new IDs are the same");
      } else if (e.getComment().startsWith("branch: Created from")) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment starts with 'branch: Created from'");
      } else if (e.getComment().equals("branch: Reset to " + branchName)) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment is 'branch: Reset to ${branchName}'");
      } else if (e.getComment().equals("branch: Reset to HEAD")) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment is 'branch: Reset to HEAD'");
      } else if (e.getComment().startsWith("reset: moving to ")) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment starts with 'reset: moving to '");
      } else if (e.getComment().equals(rebaseComment)) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment is '${rebaseComment}'");
      } else {
        return false;
      }

      return true;
    };

    return entries.reject(isEntryExcluded);
  }

  /**
   * @return reflog entries, excluding branch creation and branch reset events irrelevant for fork point/upstream inference,
   *         ordered from the latest to the oldest
   * @throws GitCoreException in case of JGit error
   */
  @SuppressWarnings("regexp") // to allow `synchronized`
  protected synchronized List<ReflogEntry> deriveFilteredReflog() throws GitCoreException {
    if (filteredReflog == null) {
      try {
        ReflogReader reflogReader = repo.getJgitRepo().getReflogReader(getFullName());
        if (reflogReader == null) {
          throw new GitCoreNoSuchRevisionException("Local branch '${getFullName()}' does not exist in this repository");
        }
        filteredReflog = rejectExcludedEntries(List.ofAll(reflogReader.getReverseEntries()));
      } catch (IOException e) {
        throw new GitCoreException(e);
      }
    }
    return filteredReflog;
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
