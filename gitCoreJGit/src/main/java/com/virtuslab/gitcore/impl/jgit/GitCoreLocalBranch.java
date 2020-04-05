package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;

public class GitCoreLocalBranch extends GitCoreBranch implements IGitCoreLocalBranch {
  public static final String BRANCHES_PATH = "refs/heads/";

  public GitCoreLocalBranch(GitCoreRepository repo, String branchName) {
    super(repo, branchName);
  }

  @Override
  public String getBranchesPath() {
    return BRANCHES_PATH;
  }

  @Override
  public boolean isLocal() {
    return true;
  }

  @Override
  public String getBranchTypeString() {
    return getBranchTypeString(/* capitalized */ false);
  }

  @Override
  public String getBranchTypeString(boolean capitalized) {
    return capitalized ? "Local" : "local";
  }

  @Override
  public Optional<IGitCoreBranchTrackingStatus> deriveRemoteTrackingStatus() throws GitCoreException {
    BranchTrackingStatus ts = Try.of(() -> BranchTrackingStatus.of(repo.getJgitRepo(), getName()))
        .getOrElseThrow(e -> new GitCoreException(e));

    if (ts == null) {
      return Optional.empty();
    }

    return Optional.of(GitCoreBranchTrackingStatus.of(ts.getAheadCount(), ts.getBehindCount()));
  }

  @Override
  @SuppressWarnings({"index:assignment.type.incompatible", "index:argument.type.incompatible"})
  public Optional<IGitCoreRemoteBranch> getRemoteTrackingBranch() {
    var bc = new BranchConfig(repo.getJgitRepo().getConfig(), getName());
    String remoteName = bc.getRemoteTrackingBranch();
    if (remoteName == null) {
      return Optional.empty();
    } else {
      return Optional
          .of(new GitCoreRemoteBranch(repo, remoteName.substring(GitCoreRemoteBranch.BRANCHES_PATH.length())));
    }
  }

  @Override
  public Optional<BaseGitCoreCommit> deriveForkPoint() throws GitCoreException {
    RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    RevCommit commit = derivePointedRevCommit();
    try {
      walk.markStart(commit);
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

    List<List<ReflogEntry>> reflogEntryListsOfLocalBranches = Try.of(() -> repo.getLocalBranches().reject(this::equals)
        .map(branch -> Try.of(() -> {
          ReflogReader reflogReader = repo.getJgitRepo().getReflogReader(branch.getFullName());
          assert reflogReader != null : "Error while getting reflog reader";
          return reflogReader.getReverseEntries();
        }))
        .map(Try::get)
        .map(List::ofAll)
        .collect(List.collector()))
        .getOrElseThrow(e -> new GitCoreException(e));

    Optional<IGitCoreRemoteBranch> remoteTrackingBranch = getRemoteTrackingBranch();

    List<List<ReflogEntry>> reflogEntryListsOfRemoteBranches = Try
        .of(() -> repo.getRemoteBranches().filter(branch -> remoteTrackingBranch.filter(branch::equals).isEmpty())
            .map(branch -> Try.of(() -> {
              ReflogReader reflogReader = repo.getJgitRepo().getReflogReader(branch.getFullName());
              assert reflogReader != null : "Error while getting reflog reader";
              return reflogReader.getReverseEntries();
            }))
            .map(Try::get)
            .map(List::ofAll)
            .collect(List.collector()))
        .getOrElseThrow(e -> new GitCoreException(e));

    List<List<ReflogEntry>> reflogEntryLists = reflogEntryListsOfLocalBranches
        .appendAll(reflogEntryListsOfRemoteBranches);

    List<ReflogEntry> filteredReflogEntries = reflogEntryLists
        .flatMap(entries -> {
          ObjectId entryToExcludeNewId;
          if (entries.size() > 0) {
            ReflogEntry firstEntry = entries.get(entries.size() - 1);
            entryToExcludeNewId = firstEntry.getComment().startsWith("branch: Created from")
                ? firstEntry.getNewId()
                : ObjectId.zeroId();
          } else {
            entryToExcludeNewId = ObjectId.zeroId();
          }

          // It's necessary to exclude entry with the same hash as the first entry in reflog (if it still exists)
          // for cases like branch rename just after branch creation
          Predicate<ReflogEntry> isEntryExcluded = e -> e.getNewId().equals(entryToExcludeNewId)
              || e.getNewId().equals(e.getOldId())
              || e.getComment().startsWith("branch: Created from")
              || e.getComment().equals("branch: Reset to " + getBranchName())
              || e.getComment().equals("branch: Reset to HEAD")
              || e.getComment().startsWith("reset: moving to ")
              || e.getComment().equals("rebase finished: " + getFullName() + " onto "
                  + Try.of(() -> getPointedCommit().getHash().getHashString()).getOrElse(""));

          return entries.reject(isEntryExcluded);
        });

    for (RevCommit currentBranchCommit : walk) {
      boolean currentBranchCommitInReflogs = filteredReflogEntries
          .exists(branchReflogEntry -> currentBranchCommit.getId().equals(branchReflogEntry.getNewId()));
      if (currentBranchCommitInReflogs) {
        return Optional.of(new GitCoreCommit(currentBranchCommit));
      }
    }

    return Optional.empty();
  }
}
