package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.util.function.Predicate;

import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.checkerframework.common.aliasing.qual.Unique;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.GitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitcore.api.IGitCoreRemoteBranch;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public class GitCoreLocalBranch extends GitCoreBranch implements IGitCoreLocalBranch {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("gitCore");

  public static final String BRANCHES_PATH = Constants.R_HEADS;

  @Nullable
  private final IGitCoreRemoteBranch remoteBranch;

  public GitCoreLocalBranch(GitCoreRepository repo, String branchName, String remoteName,
      @Nullable IGitCoreRemoteBranch remoteBranch) {
    super(repo, branchName, remoteName);
    this.remoteBranch = remoteBranch;
  }

  @Override
  public String getFullName() {
    return getBranchesPath() + branchName;
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
  public Option<GitCoreBranchTrackingStatus> deriveRemoteTrackingStatus() throws GitCoreException {
    LOG.debug(() -> "Entering: branch = '${getFullName()}'");
    BranchTrackingStatus ts = Try.of(() -> BranchTrackingStatus.of(repo.getJgitRepo(), getName()))
        .getOrElseThrow(e -> new GitCoreException(e));

    if (ts == null) {
      LOG.debug("No remote tracking information found");
      return Option.none();
    }

    LOG.debug(() -> "Remote repository for this branch is named ${remoteName}");
    LOG.debug(() -> "Ahead: ${ts.getAheadCount()}; Behind: ${ts.getBehindCount()}");

    return Option.of(GitCoreBranchTrackingStatus.of(ts.getAheadCount(), ts.getBehindCount(), remoteName));
  }

  @Override
  public Option<IGitCoreRemoteBranch> getRemoteTrackingBranch() {
    return Option.of(remoteBranch);
  }

  private List<ReflogEntry> rejectExcludedEntries(List<ReflogEntry> entries) {
    LOG.debug(() -> "Entering: branch = '${getFullName()}'");
    LOG.trace("Original list of entries: ");
    entries.forEach(entry -> LOG.trace(() -> "* ${entry}"));
    ObjectId entryToExcludeNewId;
    if (entries.size() > 0) {
      ReflogEntry firstEntry = entries.get(entries.size() - 1);
      if (firstEntry.getComment().startsWith("branch: Created from")) {
        entryToExcludeNewId = firstEntry.getNewId();
        LOG.debug(
            () -> "All entries with the same hash as first entry (${firstEntry.getNewId().toString()}) will be excluded "
                + "because first entry comment starts with 'branch: Created from'");
      } else {
        entryToExcludeNewId = ObjectId.zeroId();
      }
    } else {
      entryToExcludeNewId = ObjectId.zeroId();
    }

    // It's necessary to exclude entry with the same hash as the first entry in reflog (if it still exists)
    // for cases like branch rename just after branch creation
    Predicate<ReflogEntry> isEntryExcluded = e -> {
      String rebaseMessage = "rebase finished: " + getFullName() + " onto "
          + Try.of(() -> getPointedCommit().getHash().getHashString()).getOrElse("");

      // For debug logging only
      String newIdHash = e.getNewId().getName();

      if (e.getNewId().equals(entryToExcludeNewId)) {
        LOG.debug(() -> "Exclude ${newIdHash} because it has the same hash as first entry");
      } else if (e.getNewId().equals(e.getOldId())) {
        LOG.debug(() -> "Exclude ${newIdHash} because its old and new IDs are the same");
      } else if (e.getComment().startsWith("branch: Created from")) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment starts with 'branch: Created from'");
      } else if (e.getComment().equals("branch: Reset to " + getBranchName())) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment is 'branch: Reset to ${getBranchName()}'");
      } else if (e.getComment().equals("branch: Reset to HEAD")) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment is 'branch: Reset to HEAD'");
      } else if (e.getComment().startsWith("reset: moving to ")) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment starts with 'reset: moving to '");
      } else if (e.getComment().equals(rebaseMessage)) {
        LOG.debug(() -> "Exclude ${newIdHash} because its comment is '${rebaseMessage}'");
      } else {
        return false;
      }

      return true;
    };

    return entries.reject(isEntryExcluded);
  }

  @Override
  @SuppressWarnings("aliasing:enhancedfor.type.incompatible")
  public Option<BaseGitCoreCommit> deriveForkPoint() throws GitCoreException {
    LOG.debug(() -> "Entering: branch = '${getFullName()}'");
    LOG.debug("Getting local branches reflog lists");

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

    LOG.debug("Getting remote branches reflog lists");

    Option<IGitCoreRemoteBranch> remoteTrackingBranch = getRemoteTrackingBranch();

    List<List<ReflogEntry>> reflogEntryListsOfRemoteBranches = Try
        .of(() -> repo.getAllRemoteBranches()
            .reject(branch -> remoteTrackingBranch.isDefined() && remoteTrackingBranch.get().equals(branch))
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

    List<ReflogEntry> filteredReflogEntries = reflogEntryLists.flatMap(this::rejectExcludedEntries);

    LOG.debug("Start walking through logs");

    RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    @Unique RevCommit commit = resolveRevCommit(getPointedCommit().getHash().getHashString());
    try {
      walk.markStart(commit);
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

    // There's apparently no way for AliasingChecker to work correctly with generics
    // (in particular, with enhanced `for` loops, which are essentially syntax sugar over Iterator<...>);
    // hence we need to suppress `aliasing:enhancedfor.type.incompatible` here.
    for (@Unique RevCommit currentBranchCommit : walk) {
      boolean currentBranchCommitInReflogs = filteredReflogEntries
          .exists(branchReflogEntry -> currentBranchCommit.getId().equals(branchReflogEntry.getNewId()));
      if (currentBranchCommitInReflogs) {
        LOG.debug(() -> "Commit ${currentBranchCommit.getId().getName()} found in reflogs. " +
            "Returning as fork point for branch '${getFullName()}'");
        return Option.of(new GitCoreCommit(currentBranchCommit));
      }
    }

    LOG.debug("Fork point for branch '${getFullName()}' not found");
    return Option.none();
  }
}
