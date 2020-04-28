package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.util.function.Predicate;

import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ConfigConstants;
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
  private static final LambdaLogger LOG = LambdaLoggerFactory.getLogger("gitCore");

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
  public Option<IGitCoreBranchTrackingStatus> deriveRemoteTrackingStatus() throws GitCoreException {
    LOG.debug(() -> "Entering: branch = '${getFullName()}'");
    BranchTrackingStatus ts = Try.of(() -> BranchTrackingStatus.of(repo.getJgitRepo(), getName()))
        .getOrElseThrow(e -> new GitCoreException(e));

    if (ts == null) {
      LOG.debug("No remote tracking information found");
      return Option.none();
    }

    String remoteName = repo.getJgitRepo().getConfig().getString(ConfigConstants.CONFIG_BRANCH_SECTION, getName(),
        ConfigConstants.CONFIG_KEY_REMOTE);
    LOG.debug(() -> "Remote repository for this branch is named ${remoteName}");

    LOG.debug(() -> "Ahead: ${ts.getAheadCount()}; Behind: ${ts.getBehindCount()}");

    return Option.of(GitCoreBranchTrackingStatus.of(ts.getAheadCount(), ts.getBehindCount(), remoteName));
  }

  @Override
  public Option<IGitCoreRemoteBranch> getRemoteTrackingBranch() {
    var bc = new BranchConfig(repo.getJgitRepo().getConfig(), getName());
    String remoteName = bc.getRemoteTrackingBranch();
    if (remoteName == null) {
      return Option.none();
    } else {
      @SuppressWarnings("index:argument.type.incompatible")
      String branchName = remoteName.substring(GitCoreRemoteBranch.BRANCHES_PATH.length());
      return Option.of(new GitCoreRemoteBranch(repo, branchName));
    }
  }

  private List<ReflogEntry> rejectExcludedEntries(List<ReflogEntry> entries) {
    LOG.debug(() -> "Entering: branch = '${getFullName()}'");
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
        LOG.debug(
            () -> "Exclude ${newIdHash} because its comment is 'branch: Reset to ${getBranchName()}'");
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
  public Option<BaseGitCoreCommit> deriveForkPoint() throws GitCoreException {
    LOG.debug(() -> "Entering: branch = '${getFullName()}'");
    RevWalk walk = new RevWalk(repo.getJgitRepo());
    walk.sort(RevSort.TOPO);
    RevCommit commit = derivePointedRevCommit();
    try {
      walk.markStart(commit);
    } catch (IOException e) {
      throw new GitCoreException(e);
    }

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

    List<ReflogEntry> filteredReflogEntries = reflogEntryLists.flatMap(this::rejectExcludedEntries);

    LOG.debug("Start walking through logs");

    for (RevCommit currentBranchCommit : walk) {
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
