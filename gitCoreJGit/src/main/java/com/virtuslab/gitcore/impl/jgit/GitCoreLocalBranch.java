package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.nullness.qual.Nullable;
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
  @SuppressWarnings({"assignment.type.incompatible", "argument.type.incompatible"}) // for IndexChecker only
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
  public Optional<BaseGitCoreCommit> deriveForkPoint(@Nullable BaseGitCoreCommit upstreamBranchCommit)
      throws GitCoreException {
    RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    RevCommit commit = derivePointedRevCommit();
    try {
      walk.markStart(commit);
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

    List<List<ReflogEntry>> reflogEntryListsOfLocalBranches = Try.of(() -> repo.getLocalBranches().reject(this::equals)
        .map(b -> Try.of(() -> {
          ReflogReader reflogReader = repo.getJgitRepo().getReflogReader(b.getFullName());
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

          Predicate<ReflogEntry> isEntryExcluded = e -> e.getNewId().equals(entryToExcludeNewId)
              || e.getNewId().equals(e.getOldId())
              || e.getComment().startsWith("branch: Reset to ")
              || e.getComment().startsWith("reset: moving to ");

          return entries.reject(isEntryExcluded);
        });

    BaseGitCoreCommit assumedForkPoint = null;

    for (RevCommit currentBranchCommit : walk) {
      boolean currentBranchCommitInReflogs = filteredReflogEntries
          .exists(branchReflogEntry -> currentBranchCommit.getId().equals(branchReflogEntry.getNewId()));
      if (currentBranchCommitInReflogs) {
        assumedForkPoint = new GitCoreCommit(currentBranchCommit);
        break;
      }
    }

    if (upstreamBranchCommit != null && (assumedForkPoint == null || (!repo.isAncestor(upstreamBranchCommit,
        assumedForkPoint) && repo.isAncestor(upstreamBranchCommit, getPointedCommit())))) {
      assumedForkPoint = upstreamBranchCommit;
    }

    return Optional.ofNullable(assumedForkPoint);
  }
}
