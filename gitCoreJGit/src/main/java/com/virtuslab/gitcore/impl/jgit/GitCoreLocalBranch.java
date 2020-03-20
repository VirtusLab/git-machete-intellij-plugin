package com.virtuslab.gitcore.impl.jgit;

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
  public static final String branchesPath = "refs/heads/";

  public GitCoreLocalBranch(GitCoreRepository repo, String branchName) {
    super(repo, branchName);
  }

  @Override
  public String getBranchesPath() {
    return branchesPath;
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
  public Optional<IGitCoreBranchTrackingStatus> computeRemoteTrackingStatus() throws GitCoreException {
    BranchTrackingStatus ts = Try.of(() -> BranchTrackingStatus.of(repo.getJgitRepo(), getName()))
        .getOrElseThrow(e -> new GitCoreException(e));

    if (ts == null) {
      return Optional.empty();
    }

    return Optional.of(GitCoreBranchTrackingStatus.of(ts.getAheadCount(), ts.getBehindCount()));
  }

  @Override
  public Optional<IGitCoreRemoteBranch> getRemoteTrackingBranch() {
    var bc = new BranchConfig(repo.getJgitRepo().getConfig(), getName());
    String remoteName = bc.getRemoteTrackingBranch();
    if (remoteName == null) {
      return Optional.empty();
    } else {
      return Optional
          .of(new GitCoreRemoteBranch(repo, remoteName.substring(GitCoreRemoteBranch.branchesPath.length())));
    }
  }

  @Override
  public Optional<BaseGitCoreCommit> deriveForkPoint() throws GitCoreException {
    RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    RevCommit commit = computePointedRevCommit();
    try {
      walk.markStart(commit);
    } catch (Exception e) {
      throw new GitCoreException(e);
    }

    List<List<ReflogEntry>> reflogEntryListsOfLocalBranches = Try.of(() -> repo.getLocalBranches().reject(this::equals)
        .map(b -> Try.of(() -> {
          ReflogReader reflogReader = repo.getJgitRepo().getReflogReader(b.getFullName());
          assert reflogReader != null : "Error while getting reflog reader";
          return reflogReader.getReverseEntries();
        }))
        .map(Try::get) // throwable
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
            .map(Try::get) // throwable
            .map(List::ofAll)
            .collect(List.collector()))
        .getOrElseThrow(e -> new GitCoreException(e));

    List<List<ReflogEntry>> reflogEntryLists = reflogEntryListsOfLocalBranches
        .appendAll(reflogEntryListsOfRemoteBranches);

    List<ReflogEntry> filteredReflogEntries = reflogEntryLists
        .flatMap(entries -> {
          var firstEntryNewId = entries.size() > 0 ? entries.get(entries.size() - 1).getNewId() : ObjectId.zeroId();

          Predicate<ReflogEntry> isEntryExcluded = e -> e.getNewId().equals(firstEntryNewId)
              || e.getNewId().equals(e.getOldId())
              || e.getComment().startsWith("branch: Reset to ")
              || e.getComment().startsWith("reset: moving to ");

          return entries.reject(isEntryExcluded);
        });

    for (var currentBranchCommit : walk) {
      // Checked if the old ID is not zero to exclude the first entry in reflog (just after
      // creating from other branch)
      boolean currentBranchCommitInReflogs = filteredReflogEntries
          .exists(branchReflogEntry -> currentBranchCommit.getId().equals(branchReflogEntry.getNewId()));
      if (currentBranchCommitInReflogs) {
        return Optional.of(new GitCoreCommit(currentBranchCommit));
      }
    }

    return Optional.empty();
  }
}
